#!/bin/bash

# HDFS 原始数据根路径
BASE_PATH="/data/raw"

# 计算 7 天前的日期（格式：yyyy-MM-dd）
SEVEN_DAYS_AGO=$(date -d "7 days ago" +%Y-%m-%d)

echo "删除 $BASE_PATH 下所有日期早于 $SEVEN_DAYS_AGO 的分区..."

# 遍历每个主题目录
hdfs dfs -ls $BASE_PATH | awk '{print $8}' | while read topic_path; do
    if [ -n "$topic_path" ]; then
        echo "处理主题目录: $topic_path"
        # 列出该主题下的所有日期分区
        hdfs dfs -ls $topic_path | grep "date=" | awk '{print $8}' | while read partition; do
            # 提取日期部分 (date=2026-03-01)
            date_str=$(basename $partition | sed 's/date=//')
            # 如果日期小于 SEVEN_DAYS_AGO，则删除
            if [[ "$date_str" < "$SEVEN_DAYS_AGO" ]]; then
                echo "删除过期分区: $partition"
                hdfs dfs -rm -r -skipTrash $partition
            fi
        done
    fi
done

echo "清理完成。"
