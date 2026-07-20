import { appConfig } from '@/config/app.js'
import { request } from './request.js'

export function login(merchantNo, username, password) {
	return request({
		url: '/api/auth/login',
		method: 'POST',
		data: {
			merchantNo: merchantNo.trim(),
			username: username.trim(),
			password,
			clientType: appConfig.clientType
		}
	})
}

export function logout() {
	return request({ url: '/api/auth/logout', method: 'POST' })
}
