import request from '../utils/request'

export const aiopsApi = {
  getMetrics: () => request.get('/ai/aiops/monitor'),

  healthCheck: () => request.get('/ai/aiops/health'),

  resetMetrics: () => request.post('/ai/aiops/metrics/reset'),

  detectFaults: () => request.get('/ai/aiops/detect'),

  getAllFaults: () => request.get('/ai/aiops/faults'),

  resolveFault: (faultId) => request.post(`/ai/aiops/faults/${faultId}/resolve`),

  incrementCounter: (name, delta = 1) =>
    request.post('/ai/aiops/metrics/counter', null, { params: { name, delta } }),

  recordTimer: (name, duration) =>
    request.post('/ai/aiops/metrics/timer', null, { params: { name, duration } })
}