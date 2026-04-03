package com.example

import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.feature.{VectorAssembler, StandardScaler}
import org.apache.spark.storage.StorageLevel

import java.util.Properties
import java.time.{LocalDate, DayOfWeek}
import java.time.format.DateTimeFormatter
import org.apache.hadoop.fs.{FileSystem, Path}

import java.time.temporal.WeekFields
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}

object OfflineJob {

  // ---------- MySQL 连接配置 ----------
  val mysqlUrl = "jdbc:mysql://localhost:3306/campus_log?useSSL=false&serverTimezone=Asia/Shanghai"
  val mysqlUser = "root"
  val mysqlPassword = "060201"
  val mysqlDriver = "com.mysql.cj.jdbc.Driver"

  // HDFS 原始数据根路径（与实时流写入一致）
  val basePath = "/data/raw"
  val libraryPath = s"$basePath/library-access"
  val consumePath = s"$basePath/canteen-consume"

  // Schema 定义
  val librarySchema = StructType(Seq(
    StructField("card_id", StringType),
    StructField("student_id", StringType),
    StructField("gate_id", StringType),
    StructField("action", StringType),
    StructField("timestamp", LongType)
  ))

  val consumeSchema = StructType(Seq(
    StructField("consume_id", StringType),
    StructField("student_id", StringType),
    StructField("amount", DoubleType),
    StructField("device_id", StringType),
    StructField("canteen_name", StringType),
    StructField("item", StringType),
    StructField("payment", StringType),
    StructField("timestamp", LongType)
  ))

  def main(args: Array[String]): Unit = {
    // 解析周参数，格式 YYYY-Www，例如 2026-W11，默认上周
    val weekParam = parseWeekArg(args)
    println(s"处理周: $weekParam")

    val spark = SparkSession.builder()
      .appName(s"OfflineJob-$weekParam")
      .config("spark.sql.shuffle.partitions", "4")
      .getOrCreate()
    import spark.implicits._

    // 检查 HDFS 路径是否存在
    if (!pathExists(spark, libraryPath) || !pathExists(spark, consumePath)) {
      println(s"错误：HDFS 路径不存在，请检查 $libraryPath 或 $consumePath")
      sys.exit(1)
    }

    // ---------- 读取学生维度表（广播）----------
    val studentDF = readMySQLTable("student_info")
      .selectExpr("student_id", "college_id")
      .hint("broadcast")
      .persist(StorageLevel.MEMORY_AND_DISK)

    // 计算该周的起始和结束日期（周一至周日）
    val (startDate, endDate) = getWeekDateRange(weekParam)

    // ---------- 任务1：学院周度入馆排行 ----------
    println("开始计算学院周度入馆排行...")
    val libraryRaw = spark.read.parquet(libraryPath)
      .where(s"date >= '$startDate' and date <= '$endDate'")
      .select($"value")

    val libraryParsed = libraryRaw
      .select(from_json($"value".cast("string"), librarySchema).alias("data"))
      .select("data.*")
      .filter($"action" === "in")
      .select($"student_id")

    val libraryWeekly = libraryParsed
      .join(studentDF, "student_id")
      .groupBy(lit(weekParam).alias("week"), $"college_id")
      .agg(count("*").alias("access_count"))
      .select($"week", $"college_id", $"access_count")

    // 写入 MySQL 周表（先删除该周旧数据，再追加）
    writeToMySQLWithWeekDelete(libraryWeekly, "report_library_weekly", "week", weekParam)

    // ---------- 任务2：学生周度消费统计 ----------
    println("开始计算学生周度消费统计...")
    val consumeRaw = spark.read.parquet(consumePath)
      .where(s"date >= '$startDate' and date <= '$endDate'")
      .select($"value")

    val consumeParsed = consumeRaw
      .select(from_json($"value".cast("string"), consumeSchema).alias("data"))
      .select("data.*")
      .select($"student_id", $"amount")

    val consumeWeekly = consumeParsed
      .groupBy(lit(weekParam).alias("week"), $"student_id")
      .agg(
        sum($"amount").alias("total_amount"),
        count("*").alias("transaction_count")
      )
      .select($"week", $"student_id", $"total_amount", $"transaction_count")

    writeToMySQLWithWeekDelete(consumeWeekly, "report_consume_weekly", "week", weekParam)

    // ---------- 任务3：学生画像标签（基于最近3个月消费数据，仍以当前周为基准）----------
    println("开始生成学生画像标签...")
    // 计算3个月前的起始日期（基于 endDate）
    val oneWeekAgoStart = LocalDate.parse(endDate).minusWeeks(1).toString
    val consumeHistoryRaw = spark.read.parquet(consumePath)
      .where(s"date >= '$oneWeekAgoStart' and date <= '$endDate'")
      .select($"value")

    val consumeHistory = consumeHistoryRaw
      .select(from_json($"value".cast("string"), consumeSchema).alias("data"))
      .select("data.*")
      .groupBy($"student_id")
      .agg(
        sum($"amount").alias("total_amount_1w"),
        count("*").alias("transaction_count_1w"),
        avg($"amount").alias("avg_amount_1w")
      )
      .na.fill(0)

    // 特征工程与聚类（与之前相同）
    val assembler = new VectorAssembler()
      .setInputCols(Array("total_amount_1w", "transaction_count_1w", "avg_amount_1w"))
      .setOutputCol("raw_features")
    val featureDF = assembler.transform(consumeHistory)

    val scaler = new StandardScaler()
      .setInputCol("raw_features")
      .setOutputCol("features")
      .setWithStd(true)
      .setWithMean(false)
    val scalerModel = scaler.fit(featureDF)
    val scaledDF = scalerModel.transform(featureDF)

    val kmeans = new KMeans()
      .setK(3)
      .setSeed(42L)
      .setMaxIter(100)
      .setTol(1e-6)
      .setFeaturesCol("features")
      .setPredictionCol("cluster")
    val model = kmeans.fit(scaledDF)
    val clustered = model.transform(scaledDF)

    val clusterCenters = model.clusterCenters
    val sortedClusters = clusterCenters.zipWithIndex
      .map { case (center, idx) => (idx, center(0)) }
      .sortBy(-_._2)
      .map(_._1)
    val labelMap = Map(
      sortedClusters(0) -> "高消费",
      sortedClusters(1) -> "中消费",
      sortedClusters(2) -> "低消费"
    )

    val tagDF = clustered
      .select($"student_id", $"cluster")
      .map { row =>
        val sid = row.getString(0)
        val cluster = row.getInt(1)
        val level = labelMap(cluster)
        (sid, level)
      }
      .toDF("student_id", "consumption_level")
      .withColumn("interest_tags", lit(""))
      .withColumn("last_update", current_timestamp())

    // 学生画像表仍全量覆盖（与周频率无关，可按需调整）
    writeToMySQLOverwrite(tagDF, "student_tag")

    studentDF.unpersist()
    spark.stop()
  }

  /** 解析 --week 参数，格式 YYYY-Www，若未提供则返回上周（如 2026-W10） */
  def parseWeekArg(args: Array[String]): String = {
    val pattern = "--week".r
    args.sliding(2).collectFirst {
      case Array("--week", week) if week.matches("\\d{4}-W\\d{1,2}") => week
    }.getOrElse {
      // 计算上周：当前日期所在周的上一周
      val now = LocalDate.now()
      val lastWeekDate = now.minusWeeks(1)
      val weekFields = WeekFields.ISO
      val weekNum = lastWeekDate.get(WeekFields.ISO.weekOfWeekBasedYear())
      val weekYear = lastWeekDate.get(WeekFields.ISO.weekBasedYear())
      f"$weekYear-W$weekNum%02d"
    }
  }

  /** 根据周参数（如 2026-W11）计算该周的周一和周日日期 */
  def getWeekDateRange(weekParam: String): (String, String) = {
    val weekFields = WeekFields.ISO
    // 构建格式化器，例如 "2026-W11-1" 表示周一
    val formatter = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .appendValue(weekFields.weekBasedYear(), 4)
      .appendLiteral("-W")
      .appendValue(weekFields.weekOfWeekBasedYear(), 2)
      .appendLiteral("-")
      .appendValue(weekFields.dayOfWeek(), 1) // 1 = Monday (ISO)
      .toFormatter()
    val firstDayOfWeek = LocalDate.parse(weekParam + "-1", formatter)
    val lastDayOfWeek = firstDayOfWeek.plusDays(6)
    (firstDayOfWeek.toString, lastDayOfWeek.toString)
  }

  /** 检查 HDFS 路径是否存在 */
  def pathExists(spark: SparkSession, path: String): Boolean = {
    val fs = FileSystem.get(spark.sparkContext.hadoopConfiguration)
    fs.exists(new Path(path))
  }

  /** 从 MySQL 读取表 */
  def readMySQLTable(table: String): DataFrame = {
    val props = new Properties()
    props.setProperty("user", mysqlUser)
    props.setProperty("password", mysqlPassword)
    props.setProperty("driver", mysqlDriver)
    SparkSession.active.read.jdbc(mysqlUrl, table, props)
  }

  /** 写入 MySQL 周表，先删除指定周的数据，再追加 */
  def writeToMySQLWithWeekDelete(df: DataFrame, table: String, weekCol: String, weekValue: String): Unit = {
    val props = new Properties()
    props.setProperty("user", mysqlUser)
    props.setProperty("password", mysqlPassword)
    props.setProperty("driver", mysqlDriver)

    var conn: java.sql.Connection = null
    var stmt: java.sql.Statement = null
    try {
      conn = java.sql.DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword)
      stmt = conn.createStatement()
      val deleteSql = s"DELETE FROM $table WHERE $weekCol = '$weekValue'"
      stmt.executeUpdate(deleteSql)
      println(s"已删除 $table 中 $weekCol=$weekValue 的旧数据")
    } catch {
      case e: Exception => println(s"删除旧数据失败: ${e.getMessage}")
    } finally {
      if (stmt != null) stmt.close()
      if (conn != null) conn.close()
    }

    df.write.mode(SaveMode.Append).jdbc(mysqlUrl, table, props)
    println(s"已写入 $table 周 $weekValue")
  }

  /** 写入 MySQL，全表覆盖（用于学生画像表） */
  def writeToMySQLOverwrite(df: DataFrame, table: String): Unit = {
    val props = new Properties()
    props.setProperty("user", mysqlUser)
    props.setProperty("password", mysqlPassword)
    props.setProperty("driver", mysqlDriver)

    df.write.mode(SaveMode.Overwrite).jdbc(mysqlUrl, table, props)
    println(s"已覆盖写入 $table")
  }
}
