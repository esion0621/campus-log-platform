import React from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import ParticleBackground from './components/ParticleBackground'
import Navbar from './components/Navbar'
import Dashboard from './pages/Dashboard'
import DevicePage from './pages/DevicePage'
import LogPage from './pages/LogPage'
import ReportPage from './pages/ReportPage'
import './App.css'
import AIPage from './pages/AIPage'

function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <ParticleBackground />
        <Navbar />
        <div className="page-container">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/device" element={<DevicePage />} />
            <Route path="/logs" element={<LogPage />} />
            <Route path="/reports" element={<ReportPage />} />
            <Route path="/ai" element={<AIPage />} />
          </Routes>
        </div>
      </div>
    </BrowserRouter>
  )
}

export default App
