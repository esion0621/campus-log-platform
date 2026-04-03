import React, { useEffect, useRef } from 'react'
import * as echarts from 'echarts'
import './ChartCard.css'

const ChartCard = ({ title, icon, option, theme = 'dark' }) => {
  const chartRef = useRef(null)
  const chartInstance = useRef(null)

  useEffect(() => {
    if (chartRef.current) {
      chartInstance.current = echarts.init(chartRef.current, theme)
      chartInstance.current.setOption(option)
    }
    const handleResize = () => {
      chartInstance.current?.resize({
        animation: { duration: 300 }
      })
    }
    window.addEventListener('resize', handleResize)
    return () => {
      window.removeEventListener('resize', handleResize)
      chartInstance.current?.dispose()
    }
  }, [option, theme])

  return (
    <div className="chart-card">
      <div className="card-header">
        <i className={`fas ${icon}`}></i> {title}
      </div>
      <div ref={chartRef} className="chart-container" style={{ width: '100%', height: 'calc(100% - 40px)' }}></div>
    </div>
  )
}

export default ChartCard
