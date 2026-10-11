import request from './request'

// ================= 1. 用户认证模块 =================
export const login = (data) => {
  return request.post('/user/login', null, {
    params: data
  })
}

export const register = (data) => {
  const { adminKey, ...restData } = data
  return request.post('/user/register', restData, {
    params: {
      inputKey: adminKey || ''
    }
  })
}

export const registerUser = (data) => {
  return register(data)
}

// ================= 2. 物品管理模块 =================
export const addItem = (data) => {
  return request.post('/item/add', data)
}

export const getList = () => {
  return request.get('/item/list')
}

export const getPending = () => {
  return request.get('/item/pending')
}

export const review = (id, status) => {
  return request.post('/item/review', null, {
    params: { id, status }
  })
}

export const getStats = () => {
  return request.get('/item/stats')
}

export const getMyItems = () => {
  return request.get('/item/my')
}

export const deleteItem = (id) => {
  return request.delete(`/item/delete/${id}`)
}

// ================= 3. 认领流转模块 =================
export const claim = (data) => {
  return request.post('/claim/add', data)
}

export const getClaimList = () => {
  return request.get('/claim/list')
}

export const reviewClaim = (id, status) => {
  return request.post('/claim/review', null, {
    params: { id, status }
  })
}

export const getMyClaims = () => {
  return request.get('/claim/my')
}

// 🆕 拾获者确认认领申请
export const confirmClaim = (claimId, action) => {
  return request.post('/claim/confirm', null, {
    params: { claimId, action }
  })
}

// 🆕 获取待我确认的认领申请
export const getPendingConfirmations = () => {
  return request.get('/claim/pending-confirm')
}

// 🆕 确认归还（自动归档）
export const completeClaim = (claimId) => {
  return request.post('/claim/complete', null, {
    params: { claimId }
  })
}

// 🆕 查看联系方式
export const getContactInfo = (claimId) => {
  return request.get(`/claim/contact/${claimId}`)
}

// ================= 4. 智能匹配模块 =================
export const smartMatch = (item) => {
  return request.post('/item/match', item)
}


