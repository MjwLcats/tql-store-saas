import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const tabBarSource = await readFile(new URL('../components/app-tab-bar/app-tab-bar.vue', import.meta.url), 'utf8')
const homeSource = await readFile(new URL('../pages/home/index.vue', import.meta.url), 'utf8')
const appSource = await readFile(new URL('../App.vue', import.meta.url), 'utf8')
const loginSource = await readFile(new URL('../pages/index/index.vue', import.meta.url), 'utf8')
const pagesSource = await readFile(new URL('../pages.json', import.meta.url), 'utf8')

test('tab bar emits a local state change without router timers', () => {
	assert.match(tabBarSource, /this\.\$emit\('change', index\)/)
	assert.doesNotMatch(tabBarSource, /uni\.switchTab|navigationTimer|setTimeout/)
})

test('home page owns the active tab state', () => {
	assert.match(homeSource, /@change="setActiveTab"/)
	assert.match(homeSource, /this\.activeTab = index/)
	assert.doesNotMatch(homeSource, /syncTabBar|hideTabBar|\.navigate\(/)
})

test('pages configuration no longer declares native tab pages', () => {
	assert.doesNotMatch(pagesSource, /"tabBar"\s*:/)
	assert.doesNotMatch(pagesSource, /pages\/(tasks|messages|profile)\/index/)
	assert.doesNotMatch(`${appSource}\n${loginSource}`, /uni\.hideTabBar/)
})
