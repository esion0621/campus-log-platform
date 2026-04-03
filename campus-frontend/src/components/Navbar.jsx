import React from 'react'
import { NavLink } from 'react-router-dom'
import './Navbar.css'

const Navbar = () => {
  return (
    <nav className="navbar">
      <div className="nav-logo">
        <i className="fas fa-chart-line"></i> 校园日志
      </div>
      <div className="nav-links">
        <NavLink to="/" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <i className="fas fa-tachometer-alt"></i> 实时总览
        </NavLink>
        <NavLink to="/device" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <i className="fas fa-server"></i> 设备监控
        </NavLink>
        <NavLink to="/logs" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <i className="fas fa-list"></i> 日志分析
        </NavLink>
        <NavLink to="/reports" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <i className="fas fa-chart-pie"></i> 报表统计
        </NavLink>
        <NavLink to="/ai" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <i className="fas fa-robot"></i> AI助手
        </NavLink>
      </div>
    </nav>
  )
}

export default Navbar
