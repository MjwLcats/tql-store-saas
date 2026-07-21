import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const configSource = await readFile(new URL('../config/app.js', import.meta.url), 'utf8')
const tabBarSource = await readFile(new URL('../components/app-tab-bar/app-tab-bar.vue', import.meta.url), 'utf8')
const homeSource = await readFile(new URL('../pages/home/index.vue', import.meta.url), 'utf8')
const manifestSource = await readFile(new URL('../manifest.json', import.meta.url), 'utf8')

test('app config exposes switchable company, home and auto API profiles', () => {
	assert.match(configSource, /const ACTIVE_API_PROFILE = 'company'/)
	assert.match(configSource, /company: 'http:\/\/10\.10\.5\.44:8080'/)
	assert.match(configSource, /home: 'http:\/\/172\.20\.10\.6:8080'/)
	assert.match(configSource, /auto: __TQL_API_BASE_URL__/)
})

test('iOS manifest includes local network usage permission', () => {
	assert.match(manifestSource, /"NSLocalNetworkUsageDescription"\s*:/)
})

test('tab bar badges and top eyebrow text are removed', () => {
	assert.doesNotMatch(tabBarSource, /tab-badge|tab-dot|item\.badge|item\.dot/)
	assert.doesNotMatch(homeSource, /brand-eyebrow|同庆楼 · 门店运营/)
})
