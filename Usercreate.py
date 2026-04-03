import random
import mysql.connector
from faker import Faker

# 数据库配置
MYSQL_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '060201',
    'database': 'campus_log'
}

fake = Faker('zh_CN')

# 学院ID列表（必须与college表中已存在的对应）
college_ids = ['01', '02', '03', '04', '05']
college_majors = {
    '01': ['计算机科学与技术', '软件工程', '网络工程', '信息安全'],
    '02': ['金融学', '经济学', '国际经济与贸易', '工商管理'],
    '03': ['汉语言文学', '新闻学', '历史学', '哲学'],
    '04': ['数学与应用数学', '物理学', '统计学', '信息与计算科学'],
    '05': ['法学', '知识产权', '社会工作']
}

def generate_students(num=20000):
    students = []
    for i in range(1, num + 1):
        college_id = random.choice(college_ids)
        major = random.choice(college_majors[college_id])
        # 学号：入学年份(2022) + 学院序号(2位) + 4位序号（补零）
        student_id = f"2022{college_id}{i:04d}"
        name = fake.name()
        enroll_year = 2022
        students.append((student_id, name, college_id, major, enroll_year))
    return students

def insert_students(students, batch_size=1000):
    conn = mysql.connector.connect(**MYSQL_CONFIG)
    cursor = conn.cursor()
    sql = "INSERT INTO student_info (student_id, name, college_id, major, enroll_year) VALUES (%s, %s, %s, %s, %s)"
    for i in range(0, len(students), batch_size):
        batch = students[i:i+batch_size]
        cursor.executemany(sql, batch)
        conn.commit()
        print(f"已插入 {i+len(batch)} 条")
    cursor.close()
    conn.close()
    print("完成")

if __name__ == "__main__":
    students = generate_students(20000)  # 生成2万学生
    insert_students(students)
