import React, { useState, useEffect } from 'react'
import api from '../services/api'
import './LogList.css'

const LogList = ({ topic }) => {
  const [logs, setLogs] = useState([])

  useEffect(() => {
    const fetchLogs = async () => {
      try {
        const data = await api.getLatestLogs(topic)
        // 解析每条日志的 JSON，提取时间戳和简要内容
        const parsed = data.map(item => {
          try {
            const jsonObj = JSON.parse(item.json)
            return {
              ...item,
              timestamp: jsonObj.timestamp,
              // 根据不同主题生成友好的显示内容
              content: generateContent(topic, jsonObj)
            }
          } catch (e) {
            return { ...item, timestamp: null, content: item.json }
          }
        })
        setLogs(parsed)
      } catch (error) {
        console.error('Failed to fetch logs', error)
      }
    }
    fetchLogs()
    const interval = setInterval(fetchLogs, 3000)
    return () => clearInterval(interval)
  }, [topic])

  // 根据不同主题生成友好的显示内容
  const generateContent = (topic, obj) => {
    switch (topic) {
      case 'library':
        return `${obj.student_id} ${obj.action === 'in' ? '进入' : '离开'} ${obj.gate_id || obj.location}`
      case 'edu':
        return `${obj.student_id} 执行操作: ${obj.action}`
      case 'consume':
        return `${obj.student_id} 在 ${obj.canteen_name} 消费 ¥${obj.amount?.toFixed(2)}`
      case 'device':
        return `设备 ${obj.device_id} 状态: ${obj.status}`
      case 'session':
        return `${obj.student_id} 会话事件: ${obj.event}`
      case 'borrow':
        return `${obj.student_id} 借阅: ${obj.book_title}`
      default:
        return obj.student_id || obj.log_id || obj.consume_id || JSON.stringify(obj).slice(0, 50)
    }
  }

  return (
    <div className="log-card">
      <div className="card-header">
        <i className="fas fa-list"></i> 最新日志 - {topic}
      </div>
      <div className="log-list">
        {logs.map((log, index) => (
          <div key={index} className="log-item">
            <span className="log-time">
              {log.timestamp ? new Date(log.timestamp).toLocaleTimeString() : '--:--:--'}
            </span>
            <span className="log-content" title={log.content}>{log.content}</span>
          </div>
        ))}
        {logs.length === 0 && <div className="log-empty">暂无日志</div>}
      </div>
    </div>
  )
}

export default LogList
