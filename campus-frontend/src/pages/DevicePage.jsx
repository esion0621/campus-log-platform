import React from 'react'
import DeviceStatus from '../components/DeviceStatus'
import AlertList from '../components/AlertList'
import './DevicePage.css'

const DevicePage = () => {
  return (
    <div className="device-page">
      <div className="device-header">
        <h2><i className="fas fa-server"></i> 设备监控中心</h2>
      </div>
      <div className="device-grid">
        <div className="device-status-large">
          <DeviceStatus />
        </div>
        <div className="alert-large">
          <AlertList />
        </div>
      </div>
    </div>
  )
}

export default DevicePage
