import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import { resolveApiBaseUrl } from './build/api-base-url.mjs'

export default defineConfig(() => {
	const apiBaseUrl = resolveApiBaseUrl()
	console.info(`[tql-store-app] API base URL: ${apiBaseUrl}`)

	return {
		define: {
			__TQL_API_BASE_URL__: JSON.stringify(apiBaseUrl)
		},
		plugins: [uni()]
	}
})
