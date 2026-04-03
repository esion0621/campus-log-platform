import axios from 'axios'

const API_BASE = '/api'  // 通过代理

const api = {
  // 实时指标
  getRealtimeAll: () => axios.get(`${API_BASE}/realtime/all`).then(res => res.data),

  // 图书馆
  getLibraryCount: () => axios.get(`${API_BASE}/realtime/library/count`).then(res => res.data),
  getLibraryTrend: () => axios.get(`${API_BASE}/realtime/library/trend`).then(res => res.data),

  // 教务
  getEduQps: () => axios.get(`${API_BASE}/realtime/edu/qps`).then(res => res.data),
  getEduOnlineUsers: () => axios.get(`${API_BASE}/realtime/edu/onlineUsers`).then(res => res.data),

  // 消费
  getCanteenTotal10s: () => axios.get(`${API_BASE}/realtime/canteen/total10s`).then(res => res.data),

  // 设备
  getDeviceStatus: () => axios.get(`${API_BASE}/realtime/device/status`).then(res => res.data),
  getDeviceSummary: () => axios.get(`${API_BASE}/realtime/device/summary`).then(res => res.data),

  getLibraryWeeklyRank: (week) => axios.get(`${API_BASE}/report/library/weekly-rank`, { params: { week } }).then(res => res.data),
  getConsumeWeekly: (week) => axios.get(`${API_BASE}/report/consume/weekly`, { params: { week } }).then(res => res.data),
  getStudentConsumeWeekly: (week, studentId) => axios.get(`${API_BASE}/report/consume/student-weekly`, { params: { week, studentId } }).then(res => res.data),

  // 学生画像
  getStudentProfile: (studentId) => axios.get(`${API_BASE}/student/${studentId}/profile`).then(res => res.data),

  // 最新日志
  getLatestLogs: (topic) => axios.get(`${API_BASE}/logs/${topic}`).then(res => res.data),

  // 预警
  getLatestAlerts: () => axios.get(`${API_BASE}/alerts/latest`).then(res => res.data),
  
  // AI 助手
  chat: (question) => axios.post(`${API_BASE}/ai/chat`, { question }).then(res => res.data)
}

export default api
