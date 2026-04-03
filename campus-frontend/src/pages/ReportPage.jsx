import React, { useState, useEffect } from 'react'
import * as echarts from 'echarts'
import ChartCard from '../components/ChartCard'
import api from '../services/api'
import './ReportPage.css'

// 获取当前周（格式：YYYY-Www，例如 2026-W11）
// 基于 ISO 周规则：周一为一周开始，每年的第一个周四所在周为第1周
const getCurrentWeek = () => {
  const now = new Date()
  const year = now.getFullYear()
  const start = new Date(year, 0, 1)
  // 1月1日周几？0=周日,1=周一,...6=周六
  const dayOfWeek = start.getDay() // 0-6
  // 计算偏移：如果1月1日是周一至周三，则它属于第一周；否则属于上一年的最后一周
  // 简单近似计算（演示用），实际可使用库如 moment.js
  const oneJan = new Date(year, 0, 1)
  const daysToFirstThursday = (11 - oneJan.getDay()) % 7 // 距离第一个周四的天数
  const firstThursday = new Date(year, 0, 1 + daysToFirstThursday)
  const weekNum = Math.ceil((now - firstThursday) / (7 * 24 * 60 * 60 * 1000)) + 1
  return `${year}-W${String(weekNum).padStart(2, '0')}`
}

const ReportPage = () => {
  const [libraryRank, setLibraryRank] = useState([])
  const [borrowCategory, setBorrowCategory] = useState({})
  const [canteenShare, setCanteenShare] = useState({})
  const [currentWeek, setCurrentWeek] = useState(getCurrentWeek())

  useEffect(() => {
    const fetchData = async () => {
      try {
        // 改为调用周报接口
        const rank = await api.getLibraryWeeklyRank(currentWeek)
        setLibraryRank(rank)

        const rt = await api.getRealtimeAll()
        setBorrowCategory(rt.borrowCategoryCount || {})
        setCanteenShare(rt.canteenDailyShare || {})
      } catch (error) {
        console.error('Failed to fetch report data', error)
      }
    }
    fetchData()
  }, [currentWeek])

  // 学院排行条形图
  const collegeRankOption = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '15%', right: '5%', top: 10, bottom: 20 },
    xAxis: { type: 'value', axisLabel: { color: '#9bb7e6' }, splitLine: { lineStyle: { color: '#1e3452' } } },
    yAxis: {
      type: 'category',
      data: libraryRank.map(item => item.collegeName),
      axisLabel: { color: '#d1e1ff' }
    },
    series: [{
      name: '入馆人次',
      type: 'bar',
      data: libraryRank.map(item => item.accessCount),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#3d9cff' },
          { offset: 1, color: '#9ad0ff' }
        ]),
        borderRadius: [0, 8, 8, 0]
      },
      barWidth: 14,
      label: { show: true, position: 'right', color: '#fff' }
    }]
  }

  // 兴趣标签饼图
  const interestTagsOption = {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', right: 10, top: 20, textStyle: { color: '#c0d4f0' } },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['40%', '50%'],
      data: Object.entries(borrowCategory).map(([name, value]) => ({ name, value })),
      label: { show: false },
      itemStyle: { borderRadius: 8, borderColor: '#0f1a2a', borderWidth: 2 },
      color: ['#3d9cff', '#6dc4ff', '#ffb74d', '#ff8a65', '#ba9eff', '#9ad0ff']
    }]
  }

  // 食堂消费占比环形图
  const canteenShareOption = {
    tooltip: { trigger: 'item' },
    legend: { orient: 'horizontal', bottom: 5, textStyle: { color: '#cadaff' } },
    series: [{
      type: 'pie',
      radius: ['45%', '65%'],
      data: Object.entries(canteenShare).map(([name, value]) => ({ name, value })),
      label: { show: true, position: 'outside', color: '#fff', formatter: '{b}: {d}%' },
      itemStyle: { borderRadius: 8, borderColor: '#0f1a2a', borderWidth: 2 },
      color: ['#3d9cff', '#6dc4ff', '#ffb74d', '#ff8a65', '#ba9eff']
    }]
  }

  return (
    <div className="report-page">
      <div className="report-header">
        <h2><i className="fas fa-chart-pie"></i> 周报统计</h2>
        <div className="week-display">当前周: {currentWeek}</div>
      </div>
      <div className="report-grid">
        <div className="report-item">
          <ChartCard title="学院入馆周排行" icon="fa-ranking-star" option={collegeRankOption} />
        </div>
        <div className="report-item">
          <ChartCard title="学生兴趣标签分布" icon="fa-tags" option={interestTagsOption} />
        </div>
        <div className="report-item">
          <ChartCard title="各食堂消费占比 (今日)" icon="fa-chart-pie" option={canteenShareOption} />
        </div>
      </div>
    </div>
  )
}

export default ReportPage
