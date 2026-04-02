import request from '@/utils/request'

export function createAiSession(data) {
  return request({ url: '/system/ai/chat/session', method: 'post', data: data })
}

export function listAiSession(query) {
  return request({ url: '/system/ai/chat/session/list', method: 'get', params: query })
}

export function getAiHistory(sessionId) {
  return request({ url: '/system/ai/chat/history/' + sessionId, method: 'get' })
}

export function sendAiMessage(data) {
  return request({ url: '/system/ai/chat/send', method: 'post', data: data })
}

export function getAiGatewayDiagnostics() {
  return request({ url: '/system/ai/chat/gateway/diagnostics', method: 'get' })
}

export function probeAiGateway() {
  return request({ url: '/system/ai/chat/gateway/probe', method: 'post' })
}

export function getAiConnectDraft() {
  return request({ url: '/system/ai/chat/gateway/connect-draft', method: 'get' })
}

export function testAiConnect() {
  return request({ url: '/system/ai/chat/gateway/connect-test', method: 'post' })
}
