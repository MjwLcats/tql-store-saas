import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const tabBarSource = await readFile(new URL('../components/app-tab-bar/app-tab-bar.vue', import.meta.url), 'utf8')
const homeSource = await readFile(new URL('../pages/home/index.vue', import.meta.url), 'utf8')
const appSource = await readFile(new URL('../App.vue', import.meta.url), 'utf8')

test('tab bar uses the requested text-only labels', () => {
	for (const label of ['首页', '分析', '任务', '应用', '我的']) {
		assert.match(tabBarSource, new RegExp(`text: '${label}'`))
	}
	assert.doesNotMatch(tabBarSource, /<image/)
	assert.doesNotMatch(tabBarSource, /selectedIcon:/)
	assert.match(tabBarSource, /repeat\(5, minmax\(0, 1fr\)\)/)
})

test('content pages do not repeat the active tab label at the top', () => {
	assert.doesNotMatch(homeSource, /class="page-title"/)
	assert.doesNotMatch(homeSource, /pageTitle\s*\(/)
	assert.doesNotMatch(homeSource, /class="page-header"/)
	assert.doesNotMatch(homeSource, /class="header-action"/)
	assert.doesNotMatch(homeSource, /openMessagesTab/)
	assert.match(homeSource, /margin-top: 0;/)
})

test('app shell uses shared page layout tokens', () => {
	for (const token of ['--app-page-max-width', '--app-page-gutter', '--app-page-top-gap', '--app-tabbar-reserve', '--app-section-gap', '--app-card-radius']) {
		assert.match(appSource, new RegExp(token))
	}
	assert.match(homeSource, /env\(safe-area-inset-top\) \+ var\(--app-page-top-gap\)/)
	assert.match(homeSource, /var\(--app-tabbar-reserve\) \+ env\(safe-area-inset-bottom\)/)
})

test('application workbench exposes the first inspection modules', () => {
	for (const label of ['巡店任务', '门店自检', '检查记录', '整改任务']) {
		assert.match(homeSource, new RegExp(`label: '${label}'`))
	}
	assert.match(homeSource, /data-testid="panel-applications"/)
})

test('custom tab bar is full width, safe-area aware and text only', () => {
	assert.match(tabBarSource, /left: 0;/)
	assert.match(tabBarSource, /right: 0;/)
	assert.match(tabBarSource, /border-top: 1rpx solid/)
	assert.match(tabBarSource, /env\(safe-area-inset-bottom\)/)
	assert.match(tabBarSource, /min-height: 96rpx;/)
	assert.match(tabBarSource, /<button\b/)
})
