import React, { useState, useEffect } from 'react'
import './Header.css'

const Header = () => {
  const [time, setTime] = useState(new Date())

  useEffect(() => {
    const timer = setInterval(() => setTime(new Date()), 1000)
    return () => clearInterval(timer)
  }, [])

  const formatTime = (date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hour = String(date.getHours()).padStart(2, '0')
    const minute = String(date.getMinutes()).padStart(2, '0')
    const second = String(date.getSeconds()).padStart(2, '0')
    return `${year}-${month}-${day} ${hour}:${minute}:${second}`
  }

  return (
    <header className="dashboard-header">
      <h1 className="title">
        <i className="fas fa-university" /> 校园全场景日志分析平台
        <span className="scan-line"></span>
      </h1>
      <div className="datetime">
        <span className="time-flip">{formatTime(time)}</span>
      </div>
    </header>
  )
}

export default Header
