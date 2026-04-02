import request from '@/utils/request'

export function createAiSession(data) {
  return request({
    url: '/system/ai/chat/session',
    method: 'post',
    data: data
  })
}

export function listAiSession(query) {
  return request({
    url: '/system/ai/chat/session/list',
    method: 'get',
    params: query
  })
}

export function getAiHistory(sessionId) {
  return request({
    url: '/system/ai/chat/history/' + sessionId,
    method: 'get'
  })
}

export function sendAiMessage(data) {
  return request({
    url: '/system/ai/chat/send',
    method: 'post',
    data: data
  })
}
