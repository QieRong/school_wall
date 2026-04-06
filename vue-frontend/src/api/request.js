// File: src/api/request.js
import axios from 'axios'

const request = axios.create({
    // 使用环境变量配置API地址，支持不同环境的灵活配置
    // 开发环境：.env.development 中配置
    // 生产环境：.env.production 中配置
    // 本地覆盖：.env.local 中配置（优先级最高）
    baseURL: import.meta.env.VITE_API_URL,
    timeout: 10000
})

// 请求拦截器：自动添加Token
request.interceptors.request.use(
    config => {
        // 从 user 对象中读取 token
        const userStr = localStorage.getItem('user')
        if (userStr) {
            try {
                const user = JSON.parse(userStr)
                if (user && user.token) {
                    config.headers['Authorization'] = `Bearer ${user.token}`
                }
            } catch (e) {
                console.error('解析用户信息失败:', e)
            }
        }
        return config
    },
    error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
    res => {
        // 直接返回 data，方便前端处理
        return res.data
    },
    error => {
        // 【优化】统一错误处理 - 友好的中文提示
        let message = '网络请求失败'
        let shouldRedirect = false
        
        if (!error.response && !navigator.onLine) {
            // 网络断开
            message = '📡 网络连接已断开，请检查网络设置'
        } else if (error.response) {
            const status = error.response.status
            switch (status) {
                case 400:
                    message = error.response.data?.msg || '请求参数错误'
                    break
                case 401:
                    message = '🔐 登录已过期，请重新登录'
                    localStorage.removeItem('user')
                    shouldRedirect = true
                    break
                case 403:
                    message = '🚫 您没有权限执行此操作'
                    break
                case 404:
                    message = '❓ 请求的资源不存在'
                    break
                case 500:
                    message = '😵 服务器开小差了，请稍后再试'
                    break
                case 502:
                    message = '🔧 网关错误，服务暂时不可用'
                    break
                case 503:
                    message = '⚠️ 服务暂时不可用，请稍后再试'
                    break
                case 504:
                    message = '⏱️ 网关超时，请稍后重试'
                    break
                default:
                    message = error.response.data?.msg || `请求失败 (${status})`
            }
        } else if (error.code === 'ECONNABORTED') {
            message = '⏱️ 请求超时，请稍后重试'
        } else if (error.code === 'ERR_NETWORK') {
            message = '📡 网络连接失败，请检查网络'
        }
        
        console.error('网络请求错误:', message, error)
        
        // 延迟跳转，让用户看到错误提示
        if (shouldRedirect && window.location.pathname !== '/login') {
            setTimeout(() => {
                window.location.href = '/login'
            }, 1500)
        }
        
        // 不要在这里把 Promise 吞掉，必须 reject 出去
        return Promise.reject(new Error(message))
    }
)

export default request