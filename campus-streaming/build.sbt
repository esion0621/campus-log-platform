name := "campus-streaming"
version := "1.0"
scalaVersion := "2.12.15"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % "3.1.3" % "provided",
  "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.1.3",
  "redis.clients" % "jedis" % "3.7.0",
  "org.apache.commons" % "commons-pool2" % "2.11.1"
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
