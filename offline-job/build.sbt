name := "offline-job"
version := "1.0"
scalaVersion := "2.12.15"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.1.3" % "provided",
  "org.apache.spark" %% "spark-sql" % "3.1.3" % "provided",
  "org.apache.spark" %% "spark-mllib" % "3.1.3" % "provided",
  "mysql" % "mysql-connector-java" % "8.0.33"
)


assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
