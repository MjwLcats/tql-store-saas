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
const detailApi = await readFile(
	new URL('../api/content-tasks.js', import.meta.url),
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
	assert.doesNotMatch(detailPage, /class="shot-toolbar"/)
	assert.match(detailPage, /taskRequirementSections/)
	assert.match(detailPage, /requirement-section/)
	assert.doesNotMatch(detailPage, /<text class="instruction">\{\{ task\.taskInstruction/)
	assert.doesNotMatch(detailPage, /return \[0, 1, 2\]\.map/)
	assert.doesNotMatch(detailPage, /class="primary-button" :disabled="uploadedCount === 0"/)
})

test('uploaded storyboard video renders inline with an overlay delete action', () => {
	assert.match(detailPage, /class="uploaded-video-cover"/)
	assert.match(detailPage, /class="uploaded-video-poster"/)
	assert.match(detailPage, /class="uploaded-video-delete"/)
	assert.match(detailPage, /@click\.stop="removeShotVideo\(activeShotIndex\)"/)
	assert.match(detailPage, /v-if="showSampleSection" class="sample-section"/)
	assert.match(detailPage, /class="media-card__footer uploaded-video-meta"/)
	assert.match(detailPage, /class="media-card__viewport"/)
	assert.match(detailPage, /\.uploaded-video-cover \{[^}]*height: 360rpx/)
	assert.match(detailPage, /\.media-card__viewport \{[^}]*height: 360rpx/)
	assert.doesNotMatch(detailPage, /class="uploaded-shot-video"/)
	assert.doesNotMatch(detailPage, /class="shot-toolbar"/)
})

test('uploaded storyboard stays on the current shot and advances only from the next-shot action', () => {
	assert.doesNotMatch(detailPage, /if \(index < this\.shotSlots\.length - 1 && !videos\[index \+ 1\]\) this\.activeShotIndex = index \+ 1/)
	assert.doesNotMatch(detailPage, /class="uploaded-video-next"/)
	assert.match(detailPage, /if \(this\.activeShotIndex < this\.shotSlots\.length - 1\) return '下一个分镜'/)
	assert.match(detailPage, /else if \(this\.activeShotIndex < this\.shotSlots\.length - 1\) this\.goToNextShot\(\)/)
	assert.match(detailPage, /return this\.isOriginalTask \? '去发布视频' : '合成视频'/)
	assert.match(detailPage, /this\.activeShotIndex \+= 1/)
	assert.match(detailPage, /pageScrollTo\(\{ selector: '\.shot-switcher', duration: 220 \}\)/)
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
	assert.match(detailPage, /referenceBgms/)
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
	assert.match(detailPage, /this\.isOriginalTask \? '去发布视频' : '合成视频'/)
})

test('semi-original compose step follows a mobile script-to-video checklist', () => {
	assert.match(detailPage, /shortTitle: '成片'/)
	assert.match(detailPage, /1\. 配音与字幕/)
	assert.match(detailPage, /2\. 配乐与画面/)
	assert.doesNotMatch(detailPage, /已上传分镜/)
	assert.doesNotMatch(detailPage, /缺少分镜<\/text>/)
	assert.match(detailPage, /captionsEnabled/)
	assert.match(detailPage, /transitionsEnabled/)
	assert.match(detailPage, /后台合成中/)
	assert.match(detailPage, /primaryActionDisabled/)
	assert.match(detailPage, /return '去发布视频'/)
})

test('compose page is available without calling the unfinished compose endpoint', () => {
	assert.doesNotMatch(detailPage, /startContentTaskCompose/)
	assert.doesNotMatch(detailPage, /prepareCompose/)
	assert.match(detailPage, /暂无成片视频/)
	assert.match(detailPage, /class="composed-video-player"/)
	assert.match(detailPage, /:src="composedVideoUrl"/)
	assert.match(detailPage, /if \(this\.activeStepKey === 'compose'\)[\s\S]*?findIndex\(step => step\.key === 'publish'\)/)
})

test('composed video can be downloaded and saved to the device album', () => {
	assert.match(detailPage, /上传测试视频/)
	assert.match(detailPage, /chooseComposedVideo\(\)/)
	assert.match(detailPage, /sourceType: \['album'\]/)
	assert.match(detailPage, /composedVideoUrl: localPath/)
	assert.match(detailPage, /下载视频至相册/)
	assert.match(detailPage, /async saveComposedVideo\(\)/)
	assert.match(detailPage, /uni\.downloadFile\(/)
	assert.match(detailPage, /uni\.saveVideoToPhotosAlbum\(/)
	assert.match(detailPage, /nativePlus\.gallery\.save\(nativePath, resolve, reject\)/)
	assert.match(detailPage, /convertLocalFileSystemURL/)
	assert.match(detailPage, /await this\.saveVideoFileToAlbum\(temporaryFilePath\)/)
	assert.match(detailPage, /已保存到相册/)
	assert.match(detailPage, /需要相册权限/)
	assert.ok(detailPage.includes("if (/^https?:\\/\\//i.test(this.composedVideoUrl))"))
})

test('compose supports fixed subtitles selected for every storyboard', () => {
	assert.match(detailPage, /固定字幕/)
	assert.match(detailPage, /setSubtitleMode\('FIXED'\)/)
	assert.match(detailPage, /v-for="\(subtitle, index\) in draft\.fixedSubtitles"/)
	assert.match(detailPage, /updateFixedSubtitleSelection/)
	assert.match(detailPage, /请选择每个分镜的字幕/)
})

test('compose settings use cross-platform controls instead of native iOS-style switches', () => {
	assert.doesNotMatch(detailPage, /<switch\b/)
	assert.match(detailPage, /class="state-choice-button"/)
	assert.match(detailPage, /已开启/)
	assert.match(detailPage, /已关闭/)
	assert.match(detailPage, /已选择/)
	assert.match(detailPage, /state-choice-button--active/)
})

test('material validation gates entry to the compose step from upload', () => {
	assert.match(detailPage, /@click="selectWorkbenchStep\(index\)"/)
	assert.match(detailPage, /checkMaterialsAndEnterCompose/)
	assert.match(detailPage, /materialValidated: false/)
	assert.match(detailPage, /还缺少.*个分镜素材/)
	assert.match(detailPage, /素材校验通过/)
	assert.match(detailPage, /请先在上传页检查并合成/)
})

test('storyboard duration follows backend settings and validates uploaded videos', () => {
	assert.match(detailPage, /apiItem\.minDuration \?\? apiItem\.minDurationSeconds \?\? apiItem\.durationMin/)
	assert.match(detailPage, /apiItem\.maxDuration \?\? apiItem\.maxDurationSeconds \?\? apiItem\.durationMax/)
	assert.match(detailPage, /时长要求\|时长/)
	assert.match(detailPage, /resolveDurationRule/)
	assert.match(detailPage, /validateVideoDuration\(selectedVideo, shot\)/)
	assert.match(detailPage, /视频时长不符合要求/)
	assert.match(detailPage, /this\.shotSlots\.findIndex\(\(shot, index\) => !this\.validateVideoDuration/)
	assert.match(detailPage, /maxDuration: Math\.min\(90, Math\.max\(1, Math\.ceil\(shot\?\.maxDuration \|\| 90\)\)\)/)
})

test('publish step follows backend platforms and launches the selected native app', () => {
	assert.match(detailPage, /task\?\.platforms \|\| this\.task\?\.publishPlatforms/)
	assert.match(detailPage, /发布平台\[:：\]/)
	assert.match(detailPage, /selectedPublishPlatform/)
	assert.match(detailPage, /return '去发布'/)
	assert.match(detailPage, /plus\.runtime\.launchApplication/)
	assert.match(detailPage, /com\.ss\.android\.ugc\.aweme/)
	assert.match(detailPage, /com\.tencent\.mm/)
	assert.doesNotMatch(detailPage, /@click="addPublishLink"/)
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

test('plan pause and termination states are shown consistently in app task surfaces', () => {
	assert.match(homePage, /taskStatusLabel\(task\)/)
	assert.match(homePage, /stageTone\(taskDisplayStage\(task\)\)/)
	assert.match(homePage, /task-tag--\$\{task\.tone\}/)
	assert.match(listPage, /statusLabel\(task\)/)
	assert.match(listPage, /displayStage\(task\)/)
	assert.match(detailPage, /displayStatusLabel/)
	assert.match(detailPage, /tone\(displayStage\)/)
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
