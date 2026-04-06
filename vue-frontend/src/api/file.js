import request from './request'

// 上传文件
export function uploadFile(formData) {
  return request.post('/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
