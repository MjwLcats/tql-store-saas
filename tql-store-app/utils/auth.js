const TOKEN_KEY = 'tql-store:merchant:token'
const SESSION_KEY = 'tql-store:merchant:session'

export function getToken() {
	const session = uni.getStorageSync(SESSION_KEY)
	if (session && session.expiresAt && session.expiresAt <= Date.now()) {
		clearSession()
		return ''
	}
	return uni.getStorageSync(TOKEN_KEY) || ''
}
export function saveSession(loginResult) {
	const expiresIn = Number(loginResult.expiresIn) || 0
	const session = {
		user: loginResult.user,
		expiresAt: expiresIn > 0 ? Date.now() + expiresIn * 1000 : 0
	}
	uni.setStorageSync(TOKEN_KEY, loginResult.token)
	uni.setStorageSync(SESSION_KEY, session)
}

export function getSession() {
	if (!getToken()) return null
	return uni.getStorageSync(SESSION_KEY) || null
}

export function clearSession() {
	uni.removeStorageSync(TOKEN_KEY)
	uni.removeStorageSync(SESSION_KEY)
}
