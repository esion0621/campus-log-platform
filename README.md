# 校园全场景日志分析平台

## 项目简介

本项目是一个模拟校园环境的**全场景日志采集、处理、分析与可视化平台**。系统实时采集图书馆门禁、教务访问、一卡通消费、设备状态、教务会话、图书借阅六类日志数据，通过 **Lambda 架构**同时实现实时指标计算与离线数仓建设，最终以**可视化大屏**和**智能问答助手**的形式为用户提供数据洞察服务。项目覆盖了大数据开发的全链路。

**主要功能**：
- 实时监控：图书馆在馆人数、教务系统 QPS、消费总额、设备在线率等
- 离线报表：学院入馆周排行、学生消费周统计、学生消费画像（KMeans 聚类）
- 数据服务：REST API 提供实时指标与历史报表查询
- 智能问答：基于平台数据的自然语言问答（智谱 AI GLM-5）
- 可视化大屏：深色科技感多页签大屏，支持动态刷新

## 技术栈

| 层次         | 技术                                      | 版本          |
|--------------|-------------------------------------------|---------------|
| 数据模拟     | Python, Faker, kafka-python               | 3.8           |
| 消息队列     | Kafka                                     | 3.2.0         |
| 实时计算     | Spark Structured Streaming (Scala)        | 3.1.3         |
| 离线计算     | Spark SQL, MLlib (Scala)                  | 3.1.3         |
| 数据存储     | HDFS (Hadoop 3.2.4), Redis 5.0.7, MySQL 8.0 |               |
| 后端服务     | Spring Boot 3.2.5, JPA, WebClient         | 3.2.5         |
| 前端展示     | React 18, Vite, ECharts, Axios            | 18.2.0        |
| AI 集成      | 智谱 AI GLM-5, WebClient                  | -             |
| 构建工具     | Maven (后端), SBT (Spark), npm (前端)     |               |

## 项目结构

```
campus-project/
├── campus-backend/          # Spring Boot 后端服务
│   ├── pom.xml
│   └── src/main/java/com/example/campus/
│       ├── CampusBackendApplication.java
│       ├── config/          # Redis、CORS 配置
│       ├── controller/      # REST API 控制器
│       ├── service/         # 业务逻辑（实时、报表、AI）
│       ├── repository/      # JPA 仓库
│       ├── entity/          # JPA 实体（复合主键）
│       ├── dto/             # 数据传输对象
│       └── ai/              # 智谱 AI 适配器
├── campus-frontend/         # React 前端项目
│   ├── package.json
│   ├── vite.config.js
│   ├── public/
│   └── src/
│       ├── pages/           # 页面组件（Dashboard, Device, Log, Report, AI）
│       ├── components/      # 通用组件（Header, KPICard, ChartCard...）
│       ├── services/        # API 调用封装
│       ├── styles/          # 全局样式
│       └── utils/           # 工具函数
├── campus-streaming/        # Spark Streaming 实时作业 (Scala/SBT)
│   ├── build.sbt
│   └── src/main/scala/com/example/
│       └── CampusStreamingApp.scala   # 主入口，包含所有处理器
├── offline-job/             # Spark 离线作业 (Scala/SBT)
│   ├── build.sbt
│   └── src/main/scala/com/example/
│       └── OfflineJob.scala            # 周报生成 + 学生画像聚类
├── producer/                # Python 数据模拟器（需自行创建目录）
│   └── producer_enhanced.py           # 六类日志模拟脚本
└── README.md
```


## 环境要求

- **操作系统**：Linux (Ubuntu 20.04/22.04 推荐) 
- **Java**：JDK 17（后端 & Spark）
- **Scala**：2.12.17（Spark 3.1.3 兼容版本）
- **Python**：3.8+（运行模拟器）
- **Node.js**：18.20.8（前端）
- **Hadoop**：3.2.4（伪分布式或集群）
- **Spark**：3.1.3（需与 Hadoop 版本兼容）
- **Kafka**：3.2.0
- **Redis**：5.0.7+
- **MySQL**：8.0+
- **ZooKeeper**：3.7.1（Kafka 依赖）

## 快速开始

### 1. 克隆仓库
```bash
git clone 
cd campus-project
```

### 2. 配置数据库
- 创建 MySQL 数据库 `campus_log`。
- 详细建表参考文档
- kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic library-access
- kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic edu-access
- kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic canteen-consume
- kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic device-status
- kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic edu-session
- kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic library-borrow
### 3. 配置敏感信息（占位符替换）
- `campus-backend/src/main/resources/application.yml`：修改 MySQL 密码、Redis 密码、智谱 AI API Key。
- `producer/producer_enhanced.py`：修改 MySQL 连接密码。
- `offline-job/src/main/scala/com/example/OfflineJob.scala`：修改 `mysqlPassword`。
- `campus-streaming/.../CampusStreamingApp.scala`：确认 Kafka 地址为 `localhost:9092`。

### 4. 启动基础服务
```bash
# 启动 Hadoop HDFS
start-dfs.sh
# 启动 ZooKeeper 和 Kafka
zkServer.sh start
kafka-server-start.sh config/server.properties
# 启动 Redis
redis-server
# 启动 MySQL
sudo systemctl start mysql
```

### 5. 运行数据模拟器
```bash
cd producer
pip install -r requirements.txt   # 包含 kafka-python, mysql-connector-python, faker
python producer.py       # 可选 --exam-week 开启考试周模式
```

### 6. 运行实时流作业
```bash
cd campus-streaming
sbt package
spark-submit --class com.example.CampusStreamingApp \
  --master local[*] \
  target/scala-2.12/campus-streaming_2.12-1.0.jar
```

### 7. 运行离线作业（每周一次）
```bash
cd offline-job
sbt package
spark-submit --class com.example.OfflineJob \
  --master local[*] \
  --jars mysql-connector-j-9.5.0.jar \
  target/scala-2.12/offline-job_2.12-1.0.jar \
  --week 2026-W14   # 指定周，不指定则默认上周
```

### 8. 启动后端服务
```bash
cd campus-backend
mvn clean package
java -jar target/campus-backend-1.0.0.jar
```
后端默认端口 `2005`，可通过 `application.yml` 修改。

### 9. 启动前端
```bash
cd campus-frontend
npm install
npm run dev
```
前端默认端口 `2006`，访问 `http://localhost:2006` 即可看到大屏。

## 使用说明

- **实时总览**：展示图书馆人数趋势、食堂消费占比等核心 KPI。
- **设备监控**：查看设备在线率、离线预警。
- **日志分析**：按主题查看最新日志（支持六个主题切换）。
- **报表统计**：学院入馆周排行、学生兴趣标签分布、食堂消费占比。
- **AI 助手**：输入自然语言问题（如“图书馆现在多少人？”），AI 将结合平台数据回答。

## 主要接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/realtime/all` | GET | 获取所有实时指标 |
| `/api/report/library/weekly-rank?week=2026-W14` | GET | 学院入馆周排行 |
| `/api/student/{id}/profile` | GET | 学生画像（消费等级） |
| `/api/logs/{topic}` | GET | 最新日志列表 |
| `/api/alerts/latest` | GET | 最新预警 |
| `/api/ai/chat` | POST | 智能问答（请求体 `{"question":"..."}`） |

## 常见问题

1. **实时指标为空**：检查 Redis 是否启动，以及实时流作业是否正常运行。
2. **离线作业报错“找不到表”**：确认 MySQL 中已创建 `report_library_weekly` 等表，或设置 `ddl-auto=update`。
3. **前端跨域**：后端已配置 CORS，若仍报错请检查 `WebConfig`。
4. **AI 无法回答平台数据**：检查智谱 AI API Key 是否有效，以及 `AIQueryService` 中的关键词匹配是否覆盖。

<img width="1571" height="692" alt="批注 2026-04-03 124020" src="https://github.com/user-attachments/assets/7393dc96-6caa-43eb-8e17-eb67eabc4f11" />
<img width="1601" height="778" alt="批注 2026-04-03 124123" src="https://github.com/user-attachments/assets/70e5c82b-e129-4db5-aead-440a215cdb8c" />
<img width="1608" height="791" alt="批注 2026-04-03 124222" src="https://github.com/user-attachments/assets/6c193b86-97aa-4a82-b6ce-7b23bd692bd0" />



## 致谢

- 感谢 Spring AI 社区和智谱 AI 提供的大模型 API。
- 项目灵感来源于校园信息化建设需求。


**注意**：本项目的模拟数据仅用于演示，生产环境需接入真实数据源。部署时请务必修改所有默认密码和密钥。
