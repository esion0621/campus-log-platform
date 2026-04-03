import React, { useEffect, useState, useRef } from 'react'
import './KPICard.css'

const KPICard = ({ title, value, unit, trend, trendValue, icon }) => {
  const [displayValue, setDisplayValue] = useState(value)
  const prevValueRef = useRef(value)

  useEffect(() => {
    const prev = prevValueRef.current
    if (prev !== value) {
      // 数字翻转动效（简单模拟，可以加计数器动画）
      setDisplayValue(value)
      // 闪烁效果
      const card = document.getElementById(`kpi-${title}`)
      if (card) {
        card.classList.add('flash')
        setTimeout(() => card.classList.remove('flash'), 300)
      }
    }
    prevValueRef.current = value
  }, [value, title])

  const trendIcon = trend === 'up' ? 'fa-arrow-up' : trend === 'down' ? 'fa-arrow-down' : 'fa-minus'
  const trendColor = trend === 'up' ? '#52c41a' : trend === 'down' ? '#ff4d4f' : '#faad14'

  return (
    <div className="kpi-card" id={`kpi-${title}`}>
      <div className="kpi-title">
        <i className={`fas ${icon}`} style={{ marginRight: '8px' }}></i>
        {title}
      </div>
      <div className="kpi-value">
        {displayValue.toLocaleString()}
        <span className="kpi-unit">{unit}</span>
      </div>
      <div className="kpi-trend" style={{ color: trendColor }}>
        <i className={`fas ${trendIcon}`}></i> {trendValue}
      </div>
    </div>
  )
}

export default KPICard
