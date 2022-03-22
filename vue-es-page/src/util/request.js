import axios from 'axios'
import { Message } from 'element-ui'
import qs from 'qs'

// 创建axios实例
const service = axios.create({
  baseURL: 'http://localhost:8080', // api 的 base_url
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  },
  timeout: 20000 // 请求超时时间
})

// request拦截器
service.interceptors.request.use(
  config => {
    config.data = qs.stringify(config.data)
    return config
  },
  error => {
    // Do something with request error
    Promise.reject(error)
  }
)

// response 拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code < 0) {
      Message({
        message: res.message,
        type: 'error',
        duration: 5 * 1000
      })

      // eslint-disable-next-line prefer-promise-reject-errors
      return Promise.reject('error')
    } else {
      return response.data
    }
  },
  error => {
    Message({
      message: error.message,
      type: 'error',
      duration: 5 * 1000
    })
    return Promise.reject(error)
  }
)

export default service
