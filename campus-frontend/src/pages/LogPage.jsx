import React, { useState } from 'react'
import LogList from '../components/LogList'
import './LogPage.css'

const topics = [
  { id: 'library', name: '门禁日志', icon: 'fa-door-open' },
  { id: 'edu', name: '教务访问', icon: 'fa-graduation-cap' },
  { id: 'consume', name: '消费日志', icon: 'fa-credit-card' },
  { id: 'device', name: '设备状态', icon: 'fa-wifi' },
  { id: 'session', name: '教务会话', icon: 'fa-users' },
  { id: 'borrow', name: '借阅日志', icon: 'fa-book' }
]

const LogPage = () => {
  const [activeTopic, setActiveTopic] = useState('library')

  return (
    <div className="log-page">
      <div className="log-header">
        <h2><i className="fas fa-list"></i> 日志分析</h2>
        <div className="log-tabs">
          {topics.map(topic => (
            <button
              key={topic.id}
              className={`tab-btn ${activeTopic === topic.id ? 'active' : ''}`}
              onClick={() => setActiveTopic(topic.id)}
            >
              <i className={`fas ${topic.icon}`}></i> {topic.name}
            </button>
          ))}
        </div>
      </div>
      <div className="log-content">
        <LogList topic={activeTopic} />
      </div>
    </div>
  )
}

export default LogPage
