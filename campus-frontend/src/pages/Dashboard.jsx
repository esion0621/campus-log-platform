import React, { useState, useEffect } from 'react'
import * as echarts from 'echarts'
import Header from '../components/Header'
import KPICard from '../components/KPICard'
import ChartCard from '../components/ChartCard'
import api from '../services/api'
import './Dashboard.css'

const Dashboard = () => {
  const [realtime, setRealtime] = useState({
    libraryCount: 0,
    eduQps: 0,
    canteenTotal10s: 0,
    libraryTrend: []
  })

  useEffect(() => {
    const fetchData = async () => {
      try {
        const rt = await api.getRealtimeAll()
        setRealtime(rt)
      } catch (error) {
        console.error('Failed to fetch dashboard data', error)
      }
    }
    fetchData()
    const interval = setInterval(fetchData, 5000)
    return () => clearInterval(interval)
  }, [])

  // 图书馆趋势图配置
  const libraryTrendOption = {
    tooltip: { trigger: 'axis' },
    grid: { left: '8%', right: '5%', top: 20, bottom: 20 },
    xAxis: {
      type: 'category',
      data: Array.from({ length: realtime.libraryTrend.length }, (_, i) => i * 5 + '分钟前').reverse(),
      axisLabel: { color: '#b4caf0' }
    },
    yAxis: { type: 'value', axisLabel: { color: '#b4caf0' }, splitLine: { lineStyle: { color: '#1e3452' } } },
    series: [{
      data: realtime.libraryTrend,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { width: 4, color: '#3d9cff' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(61,156,255,0.5)' },
          { offset: 1, color: 'rgba(61,156,255,0.05)' }
        ])
      },
      animationDuration: 1000
    }]
  }

  // 食堂消费占比（示例，实际可以从 realtime 中取）
  const canteenShareOption = {
    tooltip: { trigger: 'item' },
    legend: { orient: 'horizontal', bottom: 5, textStyle: { color: '#cadaff' } },
    series: [{
      type: 'pie',
      radius: ['45%', '65%'],
      data: Object.entries(realtime.canteenDailyShare || {}).map(([name, value]) => ({ name, value })),
      label: { show: true, position: 'outside', color: '#fff', formatter: '{b}: {d}%' },
      itemStyle: { borderRadius: 8, borderColor: '#0f1a2a', borderWidth: 2 },
      color: ['#3d9cff', '#6dc4ff', '#ffb74d', '#ff8a65', '#ba9eff'],
      animationDuration: 1000
    }]
  }

  return (
    <div className="dashboard-page">
      <div className="kpi-row">
        <KPICard
          title="实时在馆人数"
          value={realtime.libraryCount}
          unit="人"
          trend="up"
          trendValue="+5.2% 较昨日"
          icon="fa-door-open"
        />
        <KPICard
          title="教务系统并发"
          value={parseFloat(realtime.eduQps)}
          unit="QPS"
          trend="stable"
          trendValue="平稳"
          icon="fa-server"
        />
        <KPICard
          title="今日消费总额"
          value={parseFloat(realtime.canteenTotal10s)}
          unit="万元"
          trend="up"
          trendValue="+12% 较昨日"
          icon="fa-coins"
        />
      </div>
      <div className="charts-row">
        <div className="chart-large">
          <ChartCard title="图书馆实时人数趋势" icon="fa-chart-line" option={libraryTrendOption} />
        </div>
        <div className="chart-large">
          <ChartCard title="各食堂消费占比 (今日)" icon="fa-chart-pie" option={canteenShareOption} />
        </div>
      </div>
    </div>
  )
}

export default Dashboard
