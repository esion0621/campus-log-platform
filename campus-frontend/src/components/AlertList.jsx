import React, { useState, useEffect, useRef } from 'react'
import api from '../services/api'
import './AlertList.css'

const AlertList = () => {
  const [alerts, setAlerts] = useState([])
  const listRef = useRef()

  useEffect(() => {
    const fetchAlerts = async () => {
      try {
        const data = await api.getLatestAlerts()
        setAlerts(data)
      } catch (error) {
        console.error('Failed to fetch alerts', error)
      }
    }
    fetchAlerts()
    const interval = setInterval(fetchAlerts, 5000)
    return () => clearInterval(interval)
  }, [])

  // 自动滚动到底部（可选）或者置顶，这里保持最新在最上
  return (
    <div className="alert-card">
      <div className="card-header">
        <i className="fas fa-exclamation-triangle"></i> 实时预警
      </div>
      <div className="alert-list" ref={listRef}>
        {alerts.map((alert, index) => (
          <div key={index} className={`alert-item ${index === 0 ? 'new' : ''}`}>
            <span className={`alert-level level-${alert.level}`}></span>
            <span className="alert-device">{alert.device}</span>
            <span className="alert-message">{alert.message}</span>
            <span className="alert-time">{new Date(alert.time).toLocaleTimeString()}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

export default AlertList
