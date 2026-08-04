import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const listPage = await readFile(
	new URL('../pages/content-tasks/index.vue', import.meta.url),
	'utf8'
)
const detailPage = await readFile(
	new URL('../pages/content-tasks/detail.vue', import.meta.url),
	'utf8'
)
const playerPage = await readFile(
	new URL('../pages/content-tasks/player.vue', import.meta.url),
	'utf8'
)
const pagesConfig = await readFile(
	new URL('../pages.json', import.meta.url),
	'utf8'
)
const homePage = await readFile(
	new URL('../pages/home/index.vue', import.meta.url),
	'utf8'
)

test('content task pages keep the employee navigation visible', () => {
	assert.match(listPage, /<app-tab-bar\s+:active-index="2"/)
	assert.match(detailPage, /<app-tab-bar\s+:active-index="2"/)
	assert.match(homePage, /options\.tab/)
})

test('task list supports real-result search without seeded data', () => {
	assert.match(listPage, /v-model\.trim="keyword"/)
	assert.match(listPage, /visibleTasks/)
	assert.match(listPage, /fetchContentTasks/)
	assert.doesNotMatch(listPage, /TQL01882|王璐|赵阳|周琳/)
})

test('task detail exposes stage-driven progress', () => {
	assert.match(detailPage, /progressStep/)
	assert.match(detailPage, /progressPercent/)
	assert.match(detailPage, /READY_TO_SHOOT/)
	assert.match(detailPage, /COMPLETED/)
	assert.match(detailPage, /回传链接/)
})

test('task detail provides the mobile execution workflow instead of a placeholder action', () => {
	assert.match(detailPage, /workbenchSteps/)
	assert.match(detailPage, /uni\.chooseVideo/)
	assert.match(detailPage, /taskDraftKey/)
	assert.match(detailPage, /publishLinks/)
	assert.match(detailPage, /保存回传信息/)
	assert.doesNotMatch(detailPage, /执行能力接口待接入/)
})

test('storyboard upload follows the document-style focused shot workflow', () => {
	assert.match(detailPage, /shot-switcher/)
	assert.match(detailPage, /activeShotIndex/)
	assert.match(detailPage, /storyboardCount/)
	assert.match(detailPage, /parsedStoryboardCount/)
	assert.match(detailPage, /isOriginalTask/)
	assert.match(detailPage, /showSampleSection/)
	assert.match(detailPage, /sample-video-card/)
	assert.match(detailPage, /sampleVideoUrl/)
	assert.match(detailPage, /previewSampleVideo/)
	assert.match(detailPage, /样例：/)
	assert.match(detailPage, /配音文案/)
	assert.match(detailPage, /上一个/)
	assert.match(detailPage, /下一个/)
	assert.match(detailPage, /instruction-box--collapsed/)
	assert.doesNotMatch(detailPage, /return \[0, 1, 2\]\.map/)
	assert.doesNotMatch(detailPage, /class="primary-button" :disabled="uploadedCount === 0"/)
})

test('storyboard parser accepts relative sample asset urls from backend', () => {
	assert.ok(detailPage.includes('new RegExp(`${label}[:：]\\\\s*([^；;|｜]+)`)'))
	assert.ok(detailPage.includes('.replace(/(?:样例视频|样例封面|示例视频|示例封面|样例比例|视频比例)[:：]\\s*[^；;|｜]+/g'))
	assert.match(detailPage, /resolveAssetUrl\(apiItem\.sampleVideoUrl/)
})

test('task detail synchronizes and previews the plan video cover example', () => {
	assert.match(detailPage, /视频封面示例/)
	assert.match(detailPage, /coverTemplateUrl/)
	assert.match(detailPage, /coverTemplateAspect/)
	assert.match(detailPage, /uni\.previewImage/)
	assert.match(detailPage, /发布时请参考该封面的构图、文字层级与视觉风格/)
})

test('reference BGM uses the cross-platform inner audio context without importing the audio component', () => {
	assert.match(detailPage, /uni\.createInnerAudioContext\(\)/)
	assert.match(detailPage, /toggleReferenceBgm/)
	assert.match(detailPage, /onUnload\(\)/)
	assert.doesNotMatch(detailPage, /<audio\b/)
})

test('task detail synchronizes the selected reference BGM', () => {
	assert.match(detailPage, /参考 BGM/)
	assert.match(detailPage, /referenceBgmUrl/)
	assert.match(detailPage, /参考BGM地址/)
	assert.match(detailPage, /class="bgm-play-button"/)
})

test('sample video opens a dedicated player instead of inline tiny playback', () => {
	assert.match(detailPage, /@click="previewSampleVideo\(activeShot\)"/)
	assert.match(detailPage, /pages\/content-tasks\/player\?src=/)
	assert.doesNotMatch(detailPage, /uni\.previewMedia\(\{\s*sources: \[\{ url: shot\.sampleVideoUrl/)
	assert.doesNotMatch(detailPage, /class="sample-video"\s+[\s\S]*?:src="activeShot\.sampleVideoUrl"/)
	assert.match(playerPage, /id="samplePlayer"/)
	assert.match(playerPage, /object-fit="contain"/)
	assert.match(pagesConfig, /pages\/content-tasks\/player/)
})

test('sample video cards adapt to portrait and landscape aspect ratios', () => {
	assert.match(detailPage, /sampleAspectLabel/)
	assert.match(detailPage, /sampleCardAspectClass/)
	assert.match(detailPage, /样例比例/)
	assert.match(detailPage, /sample-video-card--portrait/)
	assert.match(detailPage, /sample-video-card--landscape/)
	assert.match(playerPage, /player-frame--portrait/)
	assert.match(playerPage, /player-frame--landscape/)
	assert.match(playerPage, /aspect/)
})

test('sample player delegates playback controls to native iOS, Android and H5 video', () => {
	assert.match(playerPage, /:controls="true"/)
	assert.match(playerPage, /:show-progress="true"/)
	assert.match(playerPage, /:show-fullscreen-btn="true"/)
	assert.match(playerPage, /:enable-progress-gesture="true"/)
	assert.doesNotMatch(playerPage, /inline-controls/)
	assert.doesNotMatch(playerPage, /inline-progress/)
	assert.doesNotMatch(playerPage, /cover-view/)
	assert.doesNotMatch(playerPage, /custom-controls/)
	assert.doesNotMatch(playerPage, /progress-slider/)
	assert.doesNotMatch(playerPage, /toggleExpanded/)
	assert.doesNotMatch(playerPage, /togglePlaybackRate/)
	assert.doesNotMatch(playerPage, /playbackRateText/)
	assert.doesNotMatch(playerPage, /requestFullScreen/)
	assert.match(playerPage, /width: 680rpx/)
	assert.match(playerPage, /justify-content: center/)
})

test('original content tasks hide sample videos and keep one upload slot', () => {
	assert.match(detailPage, /contentCreationType/)
	assert.match(detailPage, /=== 'ORIGINAL'/)
	assert.match(detailPage, /v-if="showSampleSection" class="sample-section"/)
	assert.match(detailPage, /v-if="!isOriginalTask" class="shot-switcher"/)
	assert.match(detailPage, /isOriginalTask \? '上传视频'/)
	assert.match(detailPage, /if \(this\.isOriginalTask\) return 1/)
	assert.match(detailPage, /原创任务不配置固定分镜台词/)
})

test('task lists label original and semi-original videos', () => {
	assert.match(homePage, /creationLabel/)
	assert.match(listPage, /creationLabel\(task\)/)
	assert.match(listPage, /creation-tag/)
})

test('original content tasks skip compose and continue from upload to publish', () => {
	assert.match(detailPage, /steps\.filter\(step => step\.key !== 'compose'\)/)
	assert.match(detailPage, /activeStepKey === 'compose'/)
	assert.match(detailPage, /this\.isOriginalTask \? 'publish' : 'compose'/)
	assert.match(detailPage, /this\.isOriginalTask \? '去发布视频' : '检查并合成'/)
})

test('task detail keeps mobile buttons single-line and hides the send-step card', () => {
	assert.match(detailPage, /white-space: nowrap/)
	assert.doesNotMatch(detailPage, /send-card/)
	assert.doesNotMatch(detailPage, /视频发送步骤/)
	assert.doesNotMatch(detailPage, /01\. 拍摄素材/)
	assert.doesNotMatch(detailPage, /02\. 发布视频/)
	assert.match(detailPage, /shortTitle: '上传'/)
})

test('home task bar switches in place while explicit AI video entries open backend task pages', () => {
	assert.doesNotMatch(homePage, /if \(index === 2\)[\s\S]*?navigateTo/)
	assert.match(homePage, /this\.activeTab = index/)
	assert.match(homePage, /pages\/content-tasks\/index\?category=TODO/)
	assert.match(homePage, /AI短视频/)
	assert.match(homePage, /fetchContentTasks/)
})

test('content task header tab bars stay sticky while the page scrolls', () => {
	assert.match(listPage, /task-sticky-tabs/)
	assert.match(listPage, /position: sticky/)
	assert.match(detailPage, /\.step-scroll \{ position: sticky/)
	assert.match(detailPage, /\.shot-switcher \{ position: sticky/)
})

test('content task page titles stay fixed above sticky tab bars', () => {
	assert.match(listPage, /\.page-top \{ position: fixed/)
	assert.match(detailPage, /\.page-top \{ position: fixed/)
	assert.match(listPage, /top: calc\(env\(safe-area-inset-top\) \+ 112rpx\)/)
	assert.match(detailPage, /top: calc\(env\(safe-area-inset-top\) \+ 104rpx\)/)
})
