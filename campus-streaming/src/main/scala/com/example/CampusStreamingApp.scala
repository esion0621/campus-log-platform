package com.example

import org.apache.spark.sql.types._
import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}
import org.apache.spark.sql.streaming.{GroupState, GroupStateTimeout, OutputMode, Trigger}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.Encoders

// ==================== Schema定义 ====================
object LogSchemas {
  val librarySchema: StructType = StructType(Seq(
    StructField("card_id", StringType),
    StructField("student_id", StringType),
    StructField("gate_id", StringType),
    StructField("action", StringType),
    StructField("timestamp", LongType)
  ))

  val eduSchema: StructType = StructType(Seq(
    StructField("log_id", StringType),
    StructField("student_id", StringType),
    StructField("action", StringType),
    StructField("ip", StringType),
    StructField("timestamp", LongType)
  ))

  val consumeSchema: StructType = StructType(Seq(
    StructField("consume_id", StringType),
    StructField("student_id", StringType),
    StructField("amount", DoubleType),
    StructField("device_id", StringType),
    StructField("canteen_name", StringType),   
    StructField("item", StringType),            
    StructField("payment", StringType),       
    StructField("timestamp", LongType)
  ))

  val deviceStatusSchema: StructType = StructType(Seq(
    StructField("device_id", StringType),
    StructField("status", StringType),
    StructField("timestamp", LongType)
  ))

  val eduSessionSchema: StructType = StructType(Seq(
    StructField("session_id", StringType),
    StructField("student_id", StringType),
    StructField("event", StringType),
    StructField("timestamp", LongType)
  ))

  val borrowSchema: StructType = StructType(Seq(
    StructField("borrow_id", StringType),
    StructField("student_id", StringType),
    StructField("book_id", StringType),
    StructField("book_title", StringType),
    StructField("category", StringType),
    StructField("borrow_date", LongType),
    StructField("return_date", LongType)
  ))

  // 用于类型安全的case class
  case class LibraryEvent(student_id: String, action: String, event_time: java.sql.Timestamp)
  case class EduEvent(log_id: String, student_id: String, action: String, ip: String, event_time: java.sql.Timestamp)
  case class ConsumeEvent(
    consume_id: String,
    student_id: String,
    amount: Double,
    device_id: String,
    canteen_name: String,   
    item: String,           
    payment: String,       
    event_time: java.sql.Timestamp
  )
  case class DeviceStatusEvent(device_id: String, status: String, event_time: java.sql.Timestamp)
  case class EduSessionEvent(session_id: String, student_id: String, event: String, event_time: java.sql.Timestamp)
  case class BorrowEvent(borrow_id: String, student_id: String, book_id: String, book_title: String, category: String, borrow_time: java.sql.Timestamp, return_time: java.sql.Timestamp)
}


object RedisClient {
  private val redisHost = "localhost"
  private val redisPort = 6379
  private val redisTimeout = 2000

  private val config = new JedisPoolConfig
  config.setMaxTotal(10)
  config.setMaxIdle(5)
  config.setMinIdle(1)
  config.setTestOnBorrow(true)
  config.setTestOnReturn(true)

  private lazy val pool = new JedisPool(config, redisHost, redisPort, redisTimeout)

  def getJedis: Jedis = pool.getResource

  def returnJedis(jedis: Jedis): Unit = {
    if (jedis != null) jedis.close()
  }

  def useJedis[T](f: Jedis => T): T = {
    val jedis = getJedis
    try {
      f(jedis)
    } finally {
      returnJedis(jedis)
    }
  }
}

object LibraryStateManager {
  case class GlobalState(students: Map[String, Boolean] = Map.empty, count: Long = 0L)

  def updateGlobalState(
      key: String,
      events: Iterator[LogSchemas.LibraryEvent],
      state: GroupState[GlobalState]
  ): Long = {
    val currentState = state.getOption.getOrElse(GlobalState())
    var students = currentState.students
    var count = currentState.count
    var changed = false

    val sortedEvents = events.toSeq.sortBy(_.event_time.getTime)
    for (event <- sortedEvents) {
      val studentId = event.student_id
      val wasInside = students.getOrElse(studentId, false)
      event.action match {
        case "in" =>
          if (!wasInside) {
            students = students.updated(studentId, true)
            count += 1
            changed = true
          }
        case "out" =>
          if (wasInside) {
            students = students.updated(studentId, false)
            count -= 1
            changed = true
          }
      }
    }

    if (changed) {
      state.update(GlobalState(students, count))
    }
    count
  }
}

// ==================== 原有LibraryStreamProcessor（未修改） ====================
object LibraryStreamProcessor {
  def process(spark: SparkSession, inputDF: DataFrame): Unit = {
    import spark.implicits._

    val parsed = inputDF
      .select(from_json($"value".cast("string"), LogSchemas.librarySchema).alias("data"))
      .select("data.*")
      .withColumn("event_time", ($"timestamp" / 1000).cast("timestamp"))
      .select($"student_id", $"action", $"event_time")
      .as[LogSchemas.LibraryEvent]

    val globalCounts = parsed
      .map(event => ("global", event))
      .groupByKey(_._1)
      .mapGroupsWithState[LibraryStateManager.GlobalState, Long](
        GroupStateTimeout.NoTimeout()
      ) {
        case (key, eventsIter, state) =>
          val events = eventsIter.map(_._2)
          LibraryStateManager.updateGlobalState(key, events, state)
      }

    val query = globalCounts.writeStream
      .outputMode(OutputMode.Update())
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .foreachBatch { (batchDF: org.apache.spark.sql.Dataset[Long], batchId: Long) =>
        batchDF.foreachPartition { rows: Iterator[Long] =>
          if (rows.nonEmpty) {
            val latestCount = rows.toSeq.last
            RedisClient.useJedis { jedis =>
              jedis.set("library:current_count", latestCount.toString)
              jedis.lpush("library:trend", s"$latestCount")
              jedis.ltrim("library:trend", 0, 11)
            }
          }
          ()
        }
        ()
      }
      .option("checkpointLocation", "/tmp/spark-checkpoint/library")
      .start()

    query
  }
}

// ==================== EduStreamProcessor ====================
object EduStreamProcessor {
  def process(spark: SparkSession, inputDF: DataFrame): Unit = {
    import spark.implicits._

    val parsed = inputDF
      .select(from_json($"value".cast("string"), LogSchemas.eduSchema).alias("data"))
      .select("data.*")
      .withColumn("event_time", ($"timestamp" / 1000).cast("timestamp"))
      .as[LogSchemas.EduEvent]

    val qps = parsed
      .withWatermark("event_time", "10 seconds")
      .groupBy(window($"event_time", "10 seconds"))
      .count()
      .withColumn("qps", $"count" / 10.0)
      .select($"qps")

    val query = qps.writeStream
      .outputMode(OutputMode.Update())
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        batchDF.foreachPartition { rows: Iterator[Row] =>
          if (rows.nonEmpty) {
            val latestQps = rows.map(_.getDouble(0)).toSeq.last
            RedisClient.useJedis { jedis =>
              jedis.set("edu:qps", latestQps.toString)
            }
          }
          ()
        }
        ()
      }
      .option("checkpointLocation", "/tmp/spark-checkpoint/edu")
      .start()

    query
  }
}

// ====================ConsumeStreamProcessor ====================
object ConsumeStreamProcessor {
  def process(spark: SparkSession, inputDF: DataFrame): Unit = {
    import spark.implicits._

    val parsed = inputDF
      .select(from_json($"value".cast("string"), LogSchemas.consumeSchema).alias("data"))
      .select("data.*")
      .withColumn("event_time", ($"timestamp" / 1000).cast("timestamp"))
      .as[LogSchemas.ConsumeEvent]

    val total = parsed
      .withWatermark("event_time", "10 seconds")
      .groupBy(window($"event_time", "10 seconds"))
      .agg(sum($"amount").as("total"))
      .select($"total")

    val query = total.writeStream
      .outputMode(OutputMode.Update())
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        batchDF.foreachPartition { rows: Iterator[Row] =>
          if (rows.nonEmpty) {
            val latestTotal = rows.map(_.getDouble(0)).toSeq.last
            RedisClient.useJedis { jedis =>
              jedis.set("canteen:total_10s", latestTotal.toString)
            }
          }
          ()
        }
        ()
      }
      .option("checkpointLocation", "/tmp/spark-checkpoint/consume")
      .start()

    query
  }
}

// ==================== onsumeDeviceAggregator ====================
object ConsumeDeviceAggregator {
  def process(spark: SparkSession, inputDF: DataFrame): Unit = {
    import spark.implicits._

    val parsed = inputDF
      .select(from_json($"value".cast("string"), LogSchemas.consumeSchema).alias("data"))
      .select("data.*")
      .withColumn("event_time", ($"timestamp" / 1000).cast("timestamp"))
      .as[LogSchemas.ConsumeEvent]

    val deviceDaily = parsed
      .withWatermark("event_time", "1 minute")
      .groupBy($"device_id", window($"event_time", "1 day"))
      .agg(sum($"amount").as("daily_total"))
      .select($"device_id", $"daily_total")

    val query = deviceDaily.writeStream
      .outputMode(OutputMode.Update())
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        batchDF.foreachPartition { rows: Iterator[Row] =>
          if (rows.nonEmpty) {
            RedisClient.useJedis { jedis =>
              rows.foreach { row =>
                val device = row.getString(0)
                val total = row.getDouble(1)
                jedis.hset("canteen:daily_total", device, total.toString)
              }
            }
          }
          ()
        }
        ()
      }
      .option("checkpointLocation", "/tmp/spark-checkpoint/consume-device")
      .start()

    query
  }
}


object RawLogWriter {
  def process(spark: SparkSession, inputDF: DataFrame, topic: String): Unit = {
    import spark.implicits._

    val redisKey = topic match {
      case "library-access" => "latest_logs:library"
      case "edu-access" => "latest_logs:edu"
      case "canteen-consume" => "latest_logs:consume"
      case "device-status"    => "latest_logs:device"
      case "edu-session"      => "latest_logs:session"
      case "library-borrow"   => "latest_logs:borrow"
      case _ => throw new IllegalArgumentException(s"Unknown topic: $topic")
    }

    val hdfsBasePath = s"/data/raw/$topic"

    val query = inputDF.writeStream
      .trigger(Trigger.ProcessingTime("1 second"))
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        val dfWithDate = batchDF.withColumn("date", to_date($"timestamp"))
        dfWithDate.write
          .mode("append")
          .partitionBy("date")
          .parquet(hdfsBasePath)

        batchDF.foreachPartition { rows: Iterator[Row] =>
          if (rows.nonEmpty) {
            RedisClient.useJedis { jedis =>
              rows.foreach { row =>
                val rawJson = row.getAs[Array[Byte]]("value")
                if (rawJson != null) {
                  val jsonStr = new String(rawJson, "UTF-8")
                  jedis.lpush(redisKey, jsonStr)
                  jedis.ltrim(redisKey, 0, 99)
                }
              }
            }
          }
          ()
        }
        ()
      }
      .option("checkpointLocation", s"/tmp/spark-checkpoint/raw-$topic")
      .start()

    query
  }
}


object DeviceStatusProcessor {
  def process(spark: SparkSession, inputDF: DataFrame): Unit = {
    import spark.implicits._
    import LogSchemas.DeviceStatusEvent

    val parsed = inputDF
      .select(from_json($"value".cast("string"), LogSchemas.deviceStatusSchema).alias("data"))
      .select("data.*")
      .withColumn("event_time", ($"timestamp" / 1000).cast("timestamp"))
      .as[DeviceStatusEvent]

    val latestStatus = parsed
      .withWatermark("event_time", "30 seconds")
      .groupBy($"device_id")
      .agg(max($"event_time").as("latest_time"), last($"status").as("status"))
      .select($"device_id", $"status")

    val query = latestStatus.writeStream
      .outputMode(OutputMode.Update())
      .trigger(Trigger.ProcessingTime("5 seconds"))
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        batchDF.foreachPartition { rows: Iterator[Row] =>
          if (rows.nonEmpty) {
            RedisClient.useJedis { jedis =>
              rows.foreach { row =>
                val device = row.getString(0)
                val status = row.getString(1)
                jedis.hset("device:status", device, status)
                if (status == "offline") {
                  val alert = s"""{"device":"$device","level":"high","message":"设备离线","time":${System.currentTimeMillis()}}"""
                  jedis.lpush("alerts:latest", alert)
                  jedis.ltrim("alerts:latest", 0, 49)
                }
              }
              // 注意：此处每次微批都重新扫描所有设备计算在线率，设备少时可行，设备多时可能有性能影响
              val statusMap = jedis.hgetAll("device:status")
              val total = statusMap.size()
              var online = 0
              val iter = statusMap.values().iterator()
              while (iter.hasNext) {
                if (iter.next() == "online") online += 1
              }
              jedis.set("device:online_count", online.toString)
              jedis.set("device:total_count", total.toString)
            }
          }
          ()
        }
        ()
      }
      .option("checkpointLocation", "/tmp/spark-checkpoint/device-status")
      .start()

    query
  }
}

// ==================== 教务会话处理器 ====================
object EduSessionProcessor {

  case class SessionState(activeSessions: Map[String, Long] = Map.empty)

  def process(spark: SparkSession, inputDF: DataFrame): Unit = {
    import spark.implicits._
    import LogSchemas.EduSessionEvent

    val parsed = inputDF
      .select(from_json($"value".cast("string"), LogSchemas.eduSessionSchema).alias("data"))
      .select("data.*")
      .withColumn("event_time", ($"timestamp" / 1000).cast("timestamp"))
      .as[EduSessionEvent]

    val onlineUsers = parsed
      .map(event => ("global", event))
      .groupByKey(_._1)
      .mapGroupsWithState(GroupStateTimeout.ProcessingTimeTimeout()) {
        (key: String, events: Iterator[(String, EduSessionEvent)], state: GroupState[SessionState]) =>
          val currentState = state.getOption.getOrElse(SessionState())
          var activeSessions = currentState.activeSessions
          val now = System.currentTimeMillis()
          events.foreach { case (_, event) =>
            event.event match {
              case "login" =>
                activeSessions += (event.student_id -> now)
              case "heartbeat" =>
                activeSessions += (event.student_id -> now)
              case "logout" =>
                activeSessions -= event.student_id
            }
          }
          activeSessions = activeSessions.filter { case (_, last) => now - last < 30 * 60 * 1000 }
          state.update(SessionState(activeSessions))
          activeSessions.size
      }

    val query = onlineUsers.writeStream
      .outputMode(OutputMode.Update())
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .foreachBatch { (batchDF: org.apache.spark.sql.Dataset[Int], batchId: Long) =>
        batchDF.foreachPartition { rows: Iterator[Int] =>
          if (rows.nonEmpty) {
            val onlineCount = rows.toSeq.last
            RedisClient.useJedis { jedis =>
              jedis.set("edu:online_users", onlineCount.toString)
            }
          }
          ()
        }
        ()
      }
      .option("checkpointLocation", "/tmp/spark-checkpoint/edu-session")
      .start()

    query
  }
}

// ==================== 借阅日志处理器 ====================
object BorrowProcessor {
  def process(spark: SparkSession, inputDF: DataFrame): Unit = {
    import spark.implicits._
    import LogSchemas.BorrowEvent

    val parsed = inputDF
      .select(from_json($"value".cast("string"), LogSchemas.borrowSchema).alias("data"))
      .select("data.*")
      .withColumn("borrow_time", ($"borrow_date" / 1000).cast("timestamp"))
      .withColumn("return_time", ($"return_date" / 1000).cast("timestamp"))
      .drop("borrow_date", "return_date")
      .as[BorrowEvent]

    val categoryCounts = parsed
      .withWatermark("borrow_time", "1 minute")
      .groupBy($"category", window($"borrow_time", "1 hour"))
      .count()
      .select($"category", $"count")

    val query = categoryCounts.writeStream
      .outputMode(OutputMode.Update())
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        batchDF.foreachPartition { rows: Iterator[Row] =>
          if (rows.nonEmpty) {
            RedisClient.useJedis { jedis =>
              rows.foreach { row =>
                val category = row.getString(0)
                val cnt = row.getLong(1)
                jedis.hincrBy("borrow:category_count", category, cnt)
              }
            }
          }
          ()
        }
        ()
      }
      .option("checkpointLocation", "/tmp/spark-checkpoint/borrow")
      .start()

    query
  }
}

// ==================== 食堂消费占比处理器 ====================
object CanteenShareProcessor {
  def process(spark: SparkSession, inputDF: DataFrame): Unit = {
    import spark.implicits._
    import LogSchemas.ConsumeEvent

    val parsed = inputDF
      .select(from_json($"value".cast("string"), LogSchemas.consumeSchema).alias("data"))
      .select("data.*")
      .withColumn("event_time", ($"timestamp" / 1000).cast("timestamp"))
      .as[ConsumeEvent]

    val dailyShare = parsed
      .withWatermark("event_time", "1 minute")
      .groupBy($"canteen_name", window($"event_time", "1 day"))
      .agg(sum($"amount").as("total"))
      .select($"canteen_name", $"total")

    val query = dailyShare.writeStream
      .outputMode(OutputMode.Update())
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        batchDF.foreachPartition { rows: Iterator[Row] =>
          if (rows.nonEmpty) {
            RedisClient.useJedis { jedis =>
              rows.foreach { row =>
                val canteen = row.getString(0)
                val total = row.getDouble(1)
                // 使用 hset 覆盖，不会累积错误
                jedis.hset("canteen:daily_share", canteen, total.toString)
              }
            }
          }
          ()
        }
        ()
      }
      .option("checkpointLocation", "/tmp/spark-checkpoint/canteen-share")
      .start()

    query
  }
}

// ==================== 热门消费项目处理器 ====================
object HotItemProcessor {
  def process(spark: SparkSession, inputDF: DataFrame): Unit = {
    import spark.implicits._
    import LogSchemas.ConsumeEvent

    val parsed = inputDF
      .select(from_json($"value".cast("string"), LogSchemas.consumeSchema).alias("data"))
      .select("data.*")
      .withColumn("event_time", ($"timestamp" / 1000).cast("timestamp"))
      .as[ConsumeEvent]

    val dailyItems = parsed
      .withWatermark("event_time", "1 minute")
      .groupBy($"item", window($"event_time", "1 day"))
      .agg(count("*").as("count"), sum($"amount").as("total"))
      .select($"item", $"count", $"total")

    val query = dailyItems.writeStream
      .outputMode(OutputMode.Update())
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        batchDF.foreachPartition { rows: Iterator[Row] =>
          if (rows.nonEmpty) {
            RedisClient.useJedis { jedis =>
              rows.foreach { row =>
                val item = row.getString(0)
                val count = row.getLong(1)
                val total = row.getDouble(2)

                jedis.hset("item:daily_count", item, count.toString)
                jedis.hset("item:daily_total", item, total.toString)
              }
            }
          }
          ()
        }
        ()
      }
      .option("checkpointLocation", "/tmp/spark-checkpoint/hot-item")
      .start()

    query
  }
}

// ==================== 主应用====================
object CampusStreamingApp {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("CampusStreaming")
      .master("local[*]")
      .config("spark.sql.shuffle.partitions", "2")
      .config("spark.sql.streaming.checkpointLocation", "/tmp/spark-checkpoint")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    val kafkaParams = Map[String, String](
      "kafka.bootstrap.servers" -> "localhost:9092",
      "startingOffsets" -> "latest"
    )

    def readFromKafka(topic: String) = {
      spark.readStream
        .format("kafka")
        .options(kafkaParams)
        .option("subscribe", topic)
        .load()
    }


    val libraryDF = readFromKafka("library-access")
    val eduDF = readFromKafka("edu-access")
    val consumeDF = readFromKafka("canteen-consume")

    val deviceStatusDF = readFromKafka("device-status")
    val eduSessionDF = readFromKafka("edu-session")
    val borrowDF = readFromKafka("library-borrow")


    val libraryQuery = LibraryStreamProcessor.process(spark, libraryDF)
    val eduQuery = EduStreamProcessor.process(spark, eduDF)
    val consumeQuery = ConsumeStreamProcessor.process(spark, consumeDF)
    val deviceQuery = ConsumeDeviceAggregator.process(spark, consumeDF) 
    val rawLibraryQuery = RawLogWriter.process(spark, libraryDF, "library-access")
    val rawEduQuery = RawLogWriter.process(spark, eduDF, "edu-access")
    val rawConsumeQuery = RawLogWriter.process(spark, consumeDF, "canteen-consume")
    val rawDeviceQuery   = RawLogWriter.process(spark, deviceStatusDF, "device-status")
    val rawEduSessionQuery = RawLogWriter.process(spark, eduSessionDF, "edu-session")
    val rawBorrowQuery   = RawLogWriter.process(spark, borrowDF, "library-borrow")


    val deviceStatusQuery = DeviceStatusProcessor.process(spark, deviceStatusDF)
    val eduSessionQuery = EduSessionProcessor.process(spark, eduSessionDF)
    val borrowQuery = BorrowProcessor.process(spark, borrowDF)


    val shareQuery = CanteenShareProcessor.process(spark, consumeDF)
    val hotItemQuery = HotItemProcessor.process(spark, consumeDF) 

    // 注意：同时启动多个流会对内存造成压力，请确保资源充足（当前配置约10+个流）
    spark.streams.awaitAnyTermination()
  }
}
