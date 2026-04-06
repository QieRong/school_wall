// File: vue-frontend/src/api/report.js
import request from '@/api/request'

export function submitReport(data) {
  return request.post('/report/create', data)
}