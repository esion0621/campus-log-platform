#!/bin/bash

# 设置日志目录
LOG_DIR=/home/hadoop/anew/offline-job/logs
mkdir -p $LOG_DIR

LOG_FILE=$LOG_DIR/offline_$(date +%Y%m%d_%H%M%S).log

spark-submit \
  --class com.example.OfflineJob \
  --master local[*] \
  --jars /home/hadoop/anew/offline-job/mysql-connector-j-9.5.0.jar \
  /home/hadoop/anew/offline-job/target/scala-2.12/offline-job_2.12-1.0.jar \
  >> $LOG_FILE 2>&1
