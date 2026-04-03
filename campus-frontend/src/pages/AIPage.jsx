import React, { useState } from 'react'
import api from '../services/api'
import './AIPage.css'

const AIPage = () => {
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!question.trim()) return
    setLoading(true)
    try {
      const response = await api.chat(question)
      setAnswer(response.answer)
    } catch (error) {
      console.error('AI chat error', error)
      setAnswer('抱歉，服务暂时不可用。')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="ai-page">
      <div className="ai-header">
        <h2><i className="fas fa-robot"></i> 智能助手</h2>
        <p>欢迎使用校园日志智能助手，您可以问我关于校园数据的任何问题。</p>
      </div>
      <div className="ai-chat-container">
        <div className="ai-answer-box">
          {answer ? answer : '等待提问...'}
        </div>
        <form onSubmit={handleSubmit} className="ai-question-form">
          <input
            type="text"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="请输入您的问题..."
            disabled={loading}
          />
          <button type="submit" disabled={loading}>
            {loading ? '思考中...' : '发送'}
          </button>
        </form>
      </div>
    </div>
  )
}

export default AIPage
