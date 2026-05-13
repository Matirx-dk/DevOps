import axios from 'axios'
import { Notification, MessageBox, Message, Loading } from 'element-ui'
import store from '@/store'
import { getToken, setToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { tansParams, blobValidate } from "@/utils/aidevops"
import cache from '@/plugins/cache'
import { saveAs } from 'file-saver'
import { refreshToken } from '@/api/login'

let downloadLoadingInstance
// 是否显示重新登录
export let isRelogin = { show: false }

// --- Token 刷新相关 ---
let isRefreshing = false
let failedQueue = [] // 等待 token 刷新后重试的请求

// 将请求加入刷新队列
const processQueue = (error, token = null) => {
  failedQueue.forEach(promise => {
    if (error) {
      promise.reject(error)
    } else {
      promise.resolve(token)
    }
  })
  failedQueue = []
}

// Axios 适配器：拦截 401 并刷新 token
const refreshAdapter = axiosAdapter => {
  return config => {
    return axiosAdapter(config).catch(error => {
      const originalRequest = error.config
      if (error.response && error.response.status === 401 && !originalRequest._retry) {
        if (isRefreshing) {
          // 已经在刷新中，将请求加入队列
          return new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject })
          }).then(token => {
            originalRequest.headers['Authorization'] = 'Bearer ' + token
            return axiosAdapter(originalRequest)
          }).catch(err => Promise.reject(err))
        }
        originalRequest._retry = true
        isRefreshing = true
        return new Promise((resolve, reject) => {
          refreshToken().then(res => {
            const newToken = res.data
            if (newToken) {
              setToken(newToken)
              store.commit('SET_TOKEN', newToken)
              processQueue(null, newToken)
              originalRequest.headers['Authorization'] = 'Bearer ' + newToken
              resolve(axiosAdapter(originalRequest))
            } else {
              processQueue(error, null)
              handleExpired()
              reject(error)
            }
          }).catch(err => {
            processQueue(err, null)
            handleExpired()
            reject(err)
          }).finally(() => {
            isRefreshing = false
          })
        })
      }
      return Promise.reject(error)
    })
  }
}

const handleExpired = () => {
  if (!isRelogin.show) {
    isRelogin.show = true
    MessageBox.confirm('登录状态已过期，请重新登录', '系统提示', {
      confirmButtonText: '重新登录',
      cancelButtonText: '留在本页',
      type: 'warning'
    }).then(() => {
      isRelogin.show = false
      store.dispatch('LogOut').then(() => {
        location.href = '/index'
      })
    }).catch(() => {
      isRelogin.show = false
    })
  }
}

axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'
// 创建axios实例
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 10000
})

// 挂载刷新 adapter
service.interceptors.adapter = refreshAdapter(service.interceptors.adapter)

// request拦截器
service.interceptors.request.use(config => {
  const isToken = (config.headers || {}).isToken === false
  const isRepeatSubmit = (config.headers || {}).repeatSubmit === false
  const interval = (config.headers || {}).interval || 1000
  if (getToken() && !isToken) {
    config.headers['Authorization'] = 'Bearer ' + getToken()
  }
  if (config.method === 'get' && config.params) {
    let url = config.url + '?' + tansParams(config.params)
    url = url.slice(0, -1)
    config.params = {}
    config.url = url
  }
  if (!isRepeatSubmit && (config.method === 'post' || config.method === 'put')) {
    const requestObj = {
      url: config.url,
      data: typeof config.data === 'object' ? JSON.stringify(config.data) : config.data,
      time: new Date().getTime()
    }
    const requestSize = Object.keys(JSON.stringify(requestObj)).length
    const limitSize = 5 * 1024 * 1024
    if (requestSize >= limitSize) {
      console.warn(`[${config.url}]: ` + '请求数据大小超出允许的5M限制，无法进行防重复提交验证。')
      return config
    }
    const sessionObj = cache.session.getJSON('sessionObj')
    if (sessionObj === undefined || sessionObj === null || sessionObj === '') {
      cache.session.setJSON('sessionObj', requestObj)
    } else {
      const s_url = sessionObj.url
      const s_data = sessionObj.data
      const s_time = sessionObj.time
      if (s_data === requestObj.data && requestObj.time - s_time < interval && s_url === requestObj.url) {
        const message = '数据正在处理，请勿重复提交'
        console.warn(`[${s_url}]: ` + message)
        return Promise.reject(new Error(message))
      } else {
        cache.session.setJSON('sessionObj', requestObj)
      }
    }
  }
  return config
}, error => {
    console.log(error)
    Promise.reject(error)
})

// 响应拦截器
service.interceptors.response.use(res => {
    const code = res.data.code || 200
    const msg = errorCode[code] || res.data.msg || errorCode['default']
    if (res.request.responseType ===  'blob' || res.request.responseType ===  'arraybuffer') {
      return res.data
    }
    if (code === 401) {
      // 401 统一由 refresh adapter 处理，这里只处理业务 code 401（非 HTTP 401）
      return Promise.reject('无效的会话，或者会话已过期，请重新登录。')
    } else if (code === 500) {
      Message({ message: msg, type: 'error' })
      return Promise.reject(new Error(msg))
    } else if (code === 601) {
      Message({ message: msg, type: 'warning' })
      return Promise.reject('error')
    } else if (code !== 200) {
      Notification.error({ title: msg })
      return Promise.reject('error')
    } else {
      return res.data
    }
  },
  error => {
    console.log('err' + error)
    // 网络错误等直接 reject，不走 401 刷新流程
    if (!error.response) {
      let { message } = error
      if (message == "Network Error") {
        message = "后端接口连接异常"
      } else if (message.includes("timeout")) {
        message = "系统接口请求超时"
      } else if (message.includes("Request failed with status code")) {
        message = "系统接口" + message.slice(-3) + "异常"
      }
      Message({ message: message, type: 'error', duration: 5 * 1000 })
    }
    return Promise.reject(error)
  }
)

// 通用下载方法
export function download(url, params, filename, config) {
  downloadLoadingInstance = Loading.service({ text: "正在下载数据，请稍候", spinner: "el-icon-loading", background: "rgba(0, 0, 0, 0.7)", })
  return service.post(url, params, {
    transformRequest: [(params) => { return tansParams(params) }],
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    responseType: 'blob',
    ...config
  }).then(async (data) => {
    const isBlob = blobValidate(data)
    if (isBlob) {
      const blob = new Blob([data])
      saveAs(blob, filename)
    } else {
      const resText = await data.text()
      const rspObj = JSON.parse(resText)
      const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default']
      Message.error(errMsg)
    }
    downloadLoadingInstance.close()
  }).catch((r) => {
    console.error(r)
    Message.error('下载文件出现错误，请联系管理员！')
    downloadLoadingInstance.close()
  })
}

export default service
