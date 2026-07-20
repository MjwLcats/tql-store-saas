import { appConfig } from '@/config/app.js'
import { clearSession, getToken } from '@/utils/auth.js'

const baseUrl = appConfig.apiBaseUrl.replace(/\/$/, '')

function getErrorMessage(response) {
	if (response && response.data && response.data.message) return response.data.message
	if (response && response.statusCode) return `请求失败（${response.statusCode}）`
	return '网络请求失败，请稍后重试'
}
export function request(options) {
	return new Promise((resolve, reject) => {
		const token = getToken()
		const header = {
			'Content-Type': 'application/json',
			'X-Client-Type': appConfig.clientType,
			...(options.header || {})
		}

		if (token) header.Authorization = `Bearer ${token}`

		uni.request({
			url: `${baseUrl}${options.url}`,
			method: options.method || 'GET',
			data: options.data,
			header,
			timeout: options.timeout || 12000,
			success(response) {
				const body = response.data || {}
				if (response.statusCode >= 200 && response.statusCode < 300 && body.code === 200) {
					resolve(body.data)
					return
				}

				if (response.statusCode === 401) clearSession()
				reject(new Error(getErrorMessage(response)))
			},
			fail(error) {
				const detail = error && error.errMsg ? error.errMsg : ''
				const message = detail.includes('timeout')
					? '连接服务器超时，请稍后重试'
					: '无法连接服务器，请检查网络或服务地址'
				reject(new Error(message))
			}
		})
	})
}
