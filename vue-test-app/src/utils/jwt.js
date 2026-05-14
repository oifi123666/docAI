export function decodeJwt(token) {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const payload = parts[1]
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(decoded)
  } catch {
    return null
  }
}

export function getUserRole() {
  const token = localStorage.getItem('accessToken')
  if (!token) return null
  const payload = decodeJwt(token)
  return payload ? payload.role : null
}

export function isAdmin() {
  return getUserRole() === 'ADMIN'
}