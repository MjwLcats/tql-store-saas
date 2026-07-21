import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const tabBarSource = await readFile(new URL('../components/app-tab-bar/app-tab-bar.vue', import.meta.url), 'utf8')
const homeSource = await readFile(new URL('../pages/home/index.vue', import.meta.url), 'utf8')

test('tab bar uses the requested text-only labels', () => {
	for (const label of ['首页', '任务', '应用', '我的']) {
		assert.match(tabBarSource, new RegExp(`text: '${label}'`))
	}
	assert.doesNotMatch(tabBarSource, /<image/)
	assert.doesNotMatch(tabBarSource, /selectedIcon:/)
	assert.match(homeSource, /\['首页', '任务', '应用', '我的'\]/)
})

test('capsule implementation is retained but commented out', () => {
	assert.match(tabBarSource, /胶囊选中背景暂时停用，保留以便后续恢复/)
	assert.match(tabBarSource, /胶囊位移逻辑暂时停用，保留以便后续恢复/)
	assert.match(tabBarSource, /\/\* 胶囊选中背景暂时停用/)
})

test('wechat-style bar is full width with no rounded capsule container', () => {
	assert.match(tabBarSource, /left: 0;/)
	assert.match(tabBarSource, /right: 0;/)
	assert.match(tabBarSource, /border-top: 1rpx solid/)
	assert.match(tabBarSource, /border-radius: 0;/)
	assert.match(tabBarSource, /height: 96rpx;/)
	assert.match(tabBarSource, /font-size: 28rpx;/)
	assert.match(tabBarSource, /font-weight: 600;/)
})
