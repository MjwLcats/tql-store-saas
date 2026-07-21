// 在这里切换接口环境：'company'、'home' 或 'auto'。
const ACTIVE_API_PROFILE = 'company'

export const apiProfiles = Object.freeze({
	company: 'http://10.10.5.44:8080',
	home: 'http://172.20.10.6:8080',
	auto: __TQL_API_BASE_URL__
})

const apiBaseUrl = apiProfiles[ACTIVE_API_PROFILE]

if (!apiBaseUrl) {
	throw new Error(`未知接口环境：${ACTIVE_API_PROFILE}`)
}

console.info(`[tql-store-app] API profile: ${ACTIVE_API_PROFILE} -> ${apiBaseUrl}`)

export const appConfig = Object.freeze({
	apiProfile: ACTIVE_API_PROFILE,
	apiBaseUrl,
	clientType: 'MERCHANT'
})
