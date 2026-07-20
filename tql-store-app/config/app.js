let apiBaseUrl = 'http://127.0.0.1:8080'

// iOS/Android 真机中的 127.0.0.1 指向手机自身，调试基座需访问开发电脑的局域网地址。
// #ifdef APP-PLUS
apiBaseUrl = 'http://192.168.0.104:8080'
// #endif

export const appConfig = Object.freeze({
	apiBaseUrl,
	clientType: 'MERCHANT'
})
