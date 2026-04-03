// 数字格式化
export const formatNumber = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num.toString()
}

// 时间格式化
export const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}:${date.getSeconds().toString().padStart(2, '0')}`
}
