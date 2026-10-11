import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 5000
})

// 🌟 请求拦截器：自动为每个请求携带 JWT 令牌
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    error => Promise.reject(error)
)

// 🌟 响应拦截器：统一处理业务错误与 401/403 鉴权错误
request.interceptors.response.use(
    response => {
        const res = response.data;

        // 修改为 != 以兼容数字和字符串类型
        if (res.code != 200) {
            // 打印出导致问题的结构，方便调试
            console.error('拦截器捕获到非200响应:', res);
            ElMessage.error(res.message || '系统发生错误');
            return Promise.reject(new Error(res.message || 'Error'));
        }

        return res.data;
    },
    error => {
        console.error('请求发生异常:', error);
        const status = error.response?.status;

        if (status === 401) {
            // 未登录 / 令牌失效：清理本地状态并跳回登录页
            ElMessage.error('登录已失效，请重新登录');
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            if (window.location.pathname !== '/login' && window.location.pathname !== '/') {
                window.location.href = '/';
            }
        } else if (status === 403) {
            // 已登录但权限不足
            ElMessage.error('权限不足，禁止访问');
        } else {
            ElMessage.error('网络连接错误');
        }

        return Promise.reject(error);
    }
)

export default request
