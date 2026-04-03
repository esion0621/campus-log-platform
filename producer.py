#!/usr/bin/env python3
"""
校园日志数据模拟器（最终版）
- 基于新数据库表生成所有日志类型
- 门禁日志：增加 location 字段
- 消费日志：增加 canteen_name, item, payment 字段
- 新增设备状态日志 (device-status)：模拟心跳，用于设备在线率
- 新增教务会话日志 (edu-session)：模拟登录/登出，用于计算并发用户数
- 新增借阅日志 (library-borrow)：从 book_info 生成借阅事件
- 维持原有时段进出平衡、活跃度、考试周模式
"""

import json
import time
import random
import threading
import argparse
from datetime import datetime, timedelta
from faker import Faker
from kafka import KafkaProducer
import mysql.connector

# ========== 配置 ==========
KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
MYSQL_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '060201',
    'database': 'campus_log'
}

# 主题名称（需先在Kafka中创建）
TOPIC_LIBRARY = 'library-access'
TOPIC_EDU = 'edu-access'
TOPIC_CONSUME = 'canteen-consume'
TOPIC_DEVICE_STATUS = 'device-status'
TOPIC_EDU_SESSION = 'edu-session'
TOPIC_BORROW = 'library-borrow'

# 速率控制（条/秒）
RATE_LIBRARY = 50
RATE_EDU = 30
RATE_CONSUME = 40
RATE_DEVICE_STATUS = 30   # 每秒30条（设备心跳）
RATE_EDU_SESSION = 5      # 每秒5个会话事件
RATE_BORROW = 10          # 每秒10条借阅记录

MAX_CAPACITY = 2000
EXAM_WEEK_MODE = False    # 由命令行参数设置

# ========== 初始化 ==========
fake = Faker('zh_CN')
producer = KafkaProducer(
    bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
    value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode('utf-8')
)

def load_student_ids():
    conn = mysql.connector.connect(**MYSQL_CONFIG)
    cursor = conn.cursor()
    cursor.execute("SELECT student_id FROM student_info")
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    return [row[0] for row in rows]

STUDENT_IDS = load_student_ids()
print(f"加载了 {len(STUDENT_IDS)} 个学生ID")

# 学生活跃度分数 (0.1 ~ 1.0)
def generate_activity():
    val = random.gauss(0.6, 0.2)
    return max(0.1, min(1.0, val))

student_activity = {sid: generate_activity() for sid in STUDENT_IDS}
print("活跃度生成完成（均值约0.6，范围0.1~1.0）")

# 加载设备信息
def load_device_info():
    conn = mysql.connector.connect(**MYSQL_CONFIG)
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT device_id, device_type, location FROM device_info")
    devices = cursor.fetchall()
    cursor.close()
    conn.close()
    gate_devices = [d['device_id'] for d in devices if d['device_type'] == 'gate']
    canteen_devices = [d['device_id'] for d in devices if d['device_type'] == 'canteen']
    server_devices = [d['device_id'] for d in devices if d['device_type'] in ('server', 'network')]
    # 设备位置映射
    device_location = {d['device_id']: d['location'] for d in devices}
    return gate_devices, canteen_devices, server_devices, device_location

GATE_IDS, CANTEEN_DEVICES, SERVER_DEVICES, DEVICE_LOCATION = load_device_info()
print(f"加载门禁设备: {len(GATE_IDS)}个, 消费终端: {len(CANTEEN_DEVICES)}个, 服务器/网络设备: {len(SERVER_DEVICES)}个")

# 加载食堂名称映射
def load_canteen_names():
    conn = mysql.connector.connect(**MYSQL_CONFIG)
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT device_id, canteen_name FROM canteen_device")
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    return {row['device_id']: row['canteen_name'] for row in rows}

CANTEEN_NAME_MAP = load_canteen_names()

# 加载图书信息
def load_books():
    conn = mysql.connector.connect(**MYSQL_CONFIG)
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT book_id, title, category FROM book_info")
    books = cursor.fetchall()
    cursor.close()
    conn.close()
    return books

BOOKS = load_books()
print(f"加载图书 {len(BOOKS)} 本")

# 学生状态管理（图书馆进出）
def get_target_occupancy_ratio(hour):
    if 7 <= hour <= 9:          # 早高峰
        return 0.8
    elif 10 <= hour <= 16:       # 白天
        return 0.6
    elif 17 <= hour <= 20:       # 晚高峰（离馆多）
        return 0.4
    elif 21 <= hour <= 23:       # 夜间
        return 0.2
    else:                        # 凌晨
        return 0.3

current_hour = datetime.now().hour
target_ratio = get_target_occupancy_ratio(current_hour)
target_initial = int(MAX_CAPACITY * target_ratio)
current_occupancy = 0
student_status = {}
random.shuffle(STUDENT_IDS) 
for i, sid in enumerate(STUDENT_IDS):
    if i < target_initial:
        student_status[sid] = 'in'
        current_occupancy += 1
    else:
        student_status[sid] = 'out'

state_lock = threading.Lock()

# 会话管理（教务在线用户）
active_sessions = {}          # student_id -> last_active_time (用于心跳)
session_lock = threading.Lock()

# 发送计数
counter_lock = threading.Lock()
sent_counts = {
    TOPIC_LIBRARY: 0,
    TOPIC_EDU: 0,
    TOPIC_CONSUME: 0,
    TOPIC_DEVICE_STATUS: 0,
    TOPIC_EDU_SESSION: 0,
    TOPIC_BORROW: 0,
    'total': 0
}
skipped_counts = 0  # 因无合适学生跳过的图书馆事件

def inc_count(topic):
    with counter_lock:
        sent_counts[topic] += 1
        sent_counts['total'] += 1

def inc_skipped():
    global skipped_counts
    with counter_lock:
        skipped_counts += 1

def get_hourly_base_prob():
    """根据当前小时返回基础进概率（不考虑容量）"""
    hour = datetime.now().hour
    if EXAM_WEEK_MODE:
        if 6 <= hour <= 22:
            return 0.85
        else:
            return 0.5
    else:
        if 7 <= hour <= 9:
            return 0.9
        elif 10 <= hour <= 16:
            return 0.7
        elif 17 <= hour <= 20:
            return 0.5
        elif 21 <= hour <= 23:
            return 0.2
        else:
            return 0.3

def generate_library_log():
    """门禁日志：新增 location 字段"""
    gate_id = random.choice(GATE_IDS)
    now_ms = int(time.time() * 1000)

    with state_lock:
        global current_occupancy
        available = MAX_CAPACITY - current_occupancy
        base_in_prob = get_hourly_base_prob()
        if available > 0:
            factor = 0.5 + 0.5 * (available / MAX_CAPACITY)
            in_prob = base_in_prob * factor
        else:
            in_prob =0.0
        in_prob = max(0.0, min(0.95, in_prob))

        if random.random() < in_prob:
            out_students = [sid for sid, status in student_status.items() if status == 'out']
            if not out_students:
                return None
            weights = [student_activity[sid] for sid in out_students]
            student_id = random.choices(out_students, weights=weights)[0]
            action = 'in'
            student_status[student_id] = 'in'
            current_occupancy += 1
        else:
            if current_occupancy <= 0:
                return None
            in_students = [sid for sid, status in student_status.items() if status == 'in']
            if not in_students:
                return None
            weights = [1.0 / student_activity[sid] for sid in in_students]
            student_id = random.choices(in_students, weights=weights)[0]
            action = 'out'
            student_status[student_id] = 'out'
            current_occupancy -= 1

    return {
        "card_id": fake.uuid4()[:8].upper(),
        "student_id": student_id,
        "gate_id": gate_id,
        "location": DEVICE_LOCATION.get(gate_id, "未知位置"),
        "action": action,
        "timestamp": now_ms
    }

def generate_edu_log():
    """教务访问日志：增加 user_agent 字段"""
    hour = datetime.now().hour
    if hour >= 22 or hour < 6:
        if random.random() > 0.1:  # 晚上10%概率生成
            return None
    
    return {
        "log_id": fake.uuid4(),
        "student_id": random.choice(STUDENT_IDS),
        "action": random.choice(["login", "query_score", "query_schedule"]),
        "ip": fake.ipv4(),
        "user_agent": fake.user_agent(),
        "timestamp": int(time.time() * 1000)
    }

def generate_consume_log():
    """消费日志：增加食堂名称、消费项目、支付方式"""
    hour = datetime.now().hour
    # 晚上22点到早上6点，消费概率降低到10%
    if hour >= 22 or hour < 6:
        if random.random() > 0.05:
            return None  # 不生成消费事件
    
    device_id = random.choice(CANTEEN_DEVICES)
    canteen_name = CANTEEN_NAME_MAP.get(device_id, "未知食堂")
    items = ["米饭", "炒菜", "面条", "麻辣香锅", "奶茶", "水果", "汉堡", "盖浇饭"]
    item = random.choice(items)
    payment = random.choice(["校园卡", "微信支付", "支付宝"])
    return {
        "consume_id": fake.uuid4(),
        "student_id": random.choice(STUDENT_IDS),
        "amount": round(random.uniform(3.0, 50.0), 2),
        "device_id": device_id,
        "canteen_name": canteen_name,
        "item": item,
        "payment": payment,
        "timestamp": int(time.time() * 1000)
    }

def generate_device_status():
    """设备状态日志：模拟心跳，95%在线"""
    # 所有设备（门禁+消费+服务器）
    all_devices = GATE_IDS + CANTEEN_DEVICES + SERVER_DEVICES
    device_id = random.choice(all_devices)
    status = "online" if random.random() < 0.95 else "offline"
    return {
        "device_id": device_id,
        "status": status,
        "timestamp": int(time.time() * 1000)
    }

def generate_edu_session():
    """教务会话事件：登录/登出/心跳，用于计算实时在线用户数"""
    student_id = random.choice(STUDENT_IDS)
    now = datetime.now()
    hour = now.hour
    now_ts = int(time.time() * 1000)
    with session_lock:
        last_active = active_sessions.get(student_id)
        if last_active and (now - last_active).total_seconds() > 10 * 60:
            del active_sessions[student_id]
            last_active = None
        
        if last_active is None:
            # 晚上22点到6点，只有10%概率登录
            if hour >= 22 or hour < 6:
                if random.random() > 0.1:
                    return None  # 不生成事件
            event = "login"
            active_sessions[student_id] = now
        else:
            if random.random() < 0.2:
                event = "logout"
                del active_sessions[student_id]
            else:
                event = "heartbeat"
                active_sessions[student_id] = now
    return {
        "session_id": fake.uuid4()[:8],
        "student_id": student_id,
        "event": event,
        "timestamp": now_ts
    }

def generate_borrow_log():
    """借阅日志"""
    if not BOOKS:
        return None
    book = random.choice(BOOKS)
    return {
        "borrow_id": fake.uuid4(),
        "student_id": random.choice(STUDENT_IDS),
        "book_id": book['book_id'],
        "book_title": book['title'],
        "category": book['category'],
        "borrow_date": int(time.time() * 1000),
        "return_date": int((datetime.now() + timedelta(days=random.randint(1, 30))).timestamp() * 1000)
    }

def send_library():
    interval = 1.0 / RATE_LIBRARY
    while True:
        try:
            log = generate_library_log()
            if log:
                producer.send(TOPIC_LIBRARY, log)
                inc_count(TOPIC_LIBRARY)
            else:
                inc_skipped()
            time.sleep(interval)
        except Exception as e:
            print(f"library error: {e}")
            time.sleep(1)

def send_edu():
    interval = 1.0 / RATE_EDU
    while True:
        try:
            log = generate_edu_log()
            if log is not None:
                producer.send(TOPIC_EDU, log)
                inc_count(TOPIC_EDU)
            time.sleep(interval)
        except Exception as e:
            print(f"edu error: {e}")
            time.sleep(1)

def send_consume():
    interval = 1.0 / RATE_CONSUME
    while True:
        try:
            log = generate_consume_log()
            if log:
                producer.send(TOPIC_CONSUME, log)
                inc_count(TOPIC_CONSUME)
            time.sleep(interval)
        except Exception as e:
            print(f"consume error: {e}")
            time.sleep(1)

def send_device_status():
    interval = 1.0 / RATE_DEVICE_STATUS
    while True:
        try:
            log = generate_device_status()
            producer.send(TOPIC_DEVICE_STATUS, log)
            inc_count(TOPIC_DEVICE_STATUS)
            time.sleep(interval)
        except Exception as e:
            print(f"device-status error: {e}")
            time.sleep(1)

def send_edu_session():
    interval = 1.0 / RATE_EDU_SESSION
    while True:
        try:
            log = generate_edu_session()
            if log is not None:
                producer.send(TOPIC_EDU_SESSION, log)
                inc_count(TOPIC_EDU_SESSION)
            time.sleep(interval)
        except Exception as e:
            print(f"edu-session error: {e}")
            time.sleep(1)

def send_borrow():
    interval = 1.0 / RATE_BORROW
    while True:
        try:
            log = generate_borrow_log()
            if log:
                producer.send(TOPIC_BORROW, log)
                inc_count(TOPIC_BORROW)
            time.sleep(interval)
        except Exception as e:
            print(f"borrow error: {e}")
            time.sleep(1)

def print_stats():
    while True:
        time.sleep(10)
        with counter_lock:
            with state_lock:
                occ = current_occupancy
            with session_lock:
                active_cnt = len(active_sessions)
            mode = "考试周模式" if EXAM_WEEK_MODE else "正常模式"
            print(f"\n[{datetime.now().strftime('%H:%M:%S')}] 实时状态 [{mode}]：")
            print(f"  图书馆在馆人数: {occ}/{MAX_CAPACITY}")
            print(f"  教务在线用户数: {active_cnt}")
            print(f"  发送统计：library={sent_counts[TOPIC_LIBRARY]}, edu={sent_counts[TOPIC_EDU]}, consume={sent_counts[TOPIC_CONSUME]}, device-status={sent_counts[TOPIC_DEVICE_STATUS]}, edu-session={sent_counts[TOPIC_EDU_SESSION]}, borrow={sent_counts[TOPIC_BORROW]}, 总计={sent_counts['total']}")
            print(f"  因无合适学生跳过: {skipped_counts}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="校园日志模拟器（最终版）")
    parser.add_argument("--exam-week", action="store_true", help="开启考试周模式")
    args = parser.parse_args()
    EXAM_WEEK_MODE = args.exam_week

    print("启动校园日志模拟器最终版")
    threads = [
        threading.Thread(target=send_library, daemon=True),
        threading.Thread(target=send_edu, daemon=True),
        threading.Thread(target=send_consume, daemon=True),
        threading.Thread(target=send_device_status, daemon=True),
        threading.Thread(target=send_edu_session, daemon=True),
        threading.Thread(target=send_borrow, daemon=True),
        threading.Thread(target=print_stats, daemon=True)
    ]
    for t in threads:
        t.start()
        
    def cleanup_sessions():
        while True:
            time.sleep(10)
            now = datetime.now()
            with session_lock:
                expired = [sid for sid, last in active_sessions.items()
                           if (now - last).total_seconds() > 10 * 60]
                for sid in expired:
                    del active_sessions[sid]
                if expired:
                    print(f"清理了 {len(expired)} 个超时会话")
    
    cleanup_thread = threading.Thread(target=cleanup_sessions, daemon=True)
    cleanup_thread.start()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\n模拟器停止")
        producer.close()
