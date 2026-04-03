import React, { useState, useEffect } from 'react'
import api from '../services/api'
import './DeviceStatus.css'

const DeviceStatus = () => {
  const [devices, setDevices] = useState([])
  const [onlineCount, setOnlineCount] = useState(0)
  const [totalCount, setTotalCount] = useState(0)

  useEffect(() => {
    const fetchStatus = async () => {
      try {
        const statusMap = await api.getDeviceStatus()
        const summary = await api.getDeviceSummary()
        const devicesList = Object.entries(statusMap).map(([id, status]) => ({ id, status }))
        setDevices(devicesList)
        setOnlineCount(parseInt(summary.online || 0))
        setTotalCount(parseInt(summary.total || 0))
      } catch (error) {
        console.error('Failed to fetch device status', error)
      }
    }
    fetchStatus()
    const interval = setInterval(fetchStatus, 5000)
    return () => clearInterval(interval)
  }, [])

  // 计算在线率
  const onlineRate = totalCount > 0 ? Math.round((onlineCount / totalCount) * 100) : 0

  return (
    <div className="device-card">
      <div className="card-header">
        <i className="fas fa-wifi"></i> 设备在线率
      </div>
      <div className="device-summary">
        <div className="device-rate">
          <div className="rate-number">{onlineRate}%</div>
          <div className="rate-detail">{onlineCount} / {totalCount}</div>
        </div>
        <div className="device-progress">
          <div className="progress-bar" style={{ width: `${onlineRate}%` }}>
            <div className="progress-glow"></div>
          </div>
        </div>
      </div>
      <div className="device-list">
        {devices.slice(0, 8).map(d => (
          <div key={d.id} className="device-item">
            <span className="device-name">{d.id}</span>
            <span className={`device-status status-${d.status}`}>
              <span className="status-dot"></span> {d.status === 'online' ? '在线' : '离线'}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}

export default DeviceStatus
