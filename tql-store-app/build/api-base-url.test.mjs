import assert from 'node:assert/strict'
import test from 'node:test'
import { resolveApiBaseUrl } from './api-base-url.mjs'

test('prefers a physical adapter and ignores virtual adapters', () => {
	const interfaces = {
		'vEthernet (WSL)': [{ address: '172.29.192.1', family: 'IPv4', internal: false }],
		'Wi-Fi': [{ address: '192.168.50.23', family: 'IPv4', internal: false }]
	}

	assert.equal(resolveApiBaseUrl({}, interfaces), 'http://192.168.50.23:8080')
})

test('allows an environment override and removes its trailing slash', () => {
	assert.equal(
		resolveApiBaseUrl({ VITE_API_BASE_URL: 'https://dev-api.example.com:8443/' }, {}),
		'https://dev-api.example.com:8443'
	)
})

test('falls back to loopback when no private address is available', () => {
	assert.equal(resolveApiBaseUrl({}, {}), 'http://127.0.0.1:8080')
})

test('rejects an invalid environment override', () => {
	assert.throws(
		() => resolveApiBaseUrl({ VITE_API_BASE_URL: 'invalid' }, {}),
		/Invalid VITE_API_BASE_URL/
	)
})
