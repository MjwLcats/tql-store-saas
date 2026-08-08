<template>
	<view class="detail-page">
		<view class="page-top">
			<button class="back-button" aria-label="返回" @click="goBack">‹</button>
			<view class="page-heading">
				<text class="page-title">任务详情</text>
			</view>
		</view>

		<view v-if="loading" class="state-card"><text>正在加载任务…</text></view>
		<view v-else-if="errorMessage" class="state-card state-card--error">
			<text class="state-title">暂时无法查看任务</text>
			<text class="state-copy">{{ errorMessage }}</text>
			<button class="primary-button state-button" @click="load">重新加载</button>
		</view>

		<template v-else-if="task">
			<view class="hero-card">
				<view class="hero-top">
					<text class="stage-tag" :class="`stage-tag--${tone(displayStage)}`">{{ displayStatusLabel }}</text>
				</view>
				<text class="task-name">{{ task.planName || '未命名任务' }}</text>
				<text class="deadline" :class="{ 'deadline--danger': isOverdue }">{{ deadlineText }}</text>
			</view>

			<scroll-view class="step-scroll" scroll-x :show-scrollbar="false">
				<view class="step-tabs">
					<button
						v-for="(step, index) in workbenchSteps"
						:key="step.title"
						class="step-tab"
						:class="{ 'step-tab--active': activeStepIndex === index }"
						@click="selectWorkbenchStep(index)"
					>
						<text class="step-tab__index">{{ index + 1 }}</text>
						<text class="step-tab__title">{{ step.shortTitle }}</text>
					</button>
				</view>
			</scroll-view>

			<view v-if="activeStepKey === 'task'" class="panel-card">
				<view class="section-head">
					<view>
						<text class="card-title">{{ isOriginalTask ? '原创视频拍摄要求' : '分镜拍摄要求' }}</text>
					</view>
					<button class="ghost-button ghost-button--mini" @click="copyText(requirementCopyText, '拍摄要求')">复制要求</button>
				</view>
				<view class="requirement-list">
					<view v-for="(section, index) in taskRequirementSections" :key="section.key" class="requirement-section">
						<view class="requirement-section__head">
							<text class="requirement-index">{{ index + 1 }}</text>
							<text class="requirement-title">{{ section.title }}</text>
						</view>
						<text class="requirement-copy">{{ section.requirement }}</text>
						<view v-if="section.voiceover" class="requirement-voiceover">
							<text class="requirement-label">台词</text>
							<text class="requirement-voiceover__copy">{{ section.voiceover }}</text>
						</view>
					</view>
				</view>
			</view>

			<view v-if="activeStepKey === 'upload'" class="panel-card">
				<view class="section-head">
					<view>
						<text class="card-title">{{ isOriginalTask ? '按要求完成素材采集' : '按分镜完成素材采集' }}</text>
					</view>
					<text class="section-count">{{ uploadedCount }}/{{ shotSlots.length }}</text>
				</view>
				<scroll-view v-if="!isOriginalTask" class="shot-switcher" scroll-x :show-scrollbar="false">
					<view class="shot-switcher__inner">
						<button
							v-for="(shot, index) in shotSlots"
							:key="shot.key"
							class="shot-pill"
							:class="{ 'shot-pill--active': activeShotIndex === index, 'shot-pill--done': draft.videos[index] }"
							@click="activeShotIndex = index"
						>
							<text class="shot-pill__dot">{{ draft.videos[index] ? '✓' : index + 1 }}</text>
							<text>{{ shot.title }}</text>
						</button>
					</view>
				</scroll-view>
				<view v-if="coverTemplateUrl" class="cover-template-card">
					<image class="cover-template-image" :class="coverTemplateAspect === 'landscape' ? 'cover-template-image--landscape' : ''" :src="coverTemplateUrl" mode="aspectFill" @click="previewCoverTemplate" />
					<view class="cover-template-copy">
						<text class="cover-template-title">视频封面示例</text>
						<text class="cover-template-desc">发布时请参考该封面的构图、文字层级与视觉风格</text>
						<text class="cover-template-tip">{{ coverTemplateAspect === 'landscape' ? '横版 16:9' : '竖版 9:16' }} · 点击查看大图</text>
					</view>
				</view>
				<view v-if="referenceBgms.length" class="bgm-reference-card">
					<text class="bgm-reference-heading">参考 BGM（{{ referenceBgms.length }} 首）</text>
					<view v-for="(bgm, index) in referenceBgms" :key="`${bgm.url}-${index}`" class="bgm-reference-item">
						<view class="bgm-reference-copy">
							<text class="bgm-reference-title">{{ bgm.name }}</text>
							<text v-if="bgm.category" class="bgm-reference-desc">{{ bgm.category }}</text>
						</view>
						<button class="bgm-play-button" @click="toggleReferenceBgm(bgm, index)">
							<text class="bgm-play-icon">{{ bgmPlaying && activeBgmIndex === index ? 'Ⅱ' : '▶' }}</text>
							<text>{{ bgmPlaying && activeBgmIndex === index ? '暂停播放' : '播放参考 BGM' }}</text>
						</button>
					</view>
				</view>
				<view v-if="referenceVoiceStyle" class="voice-reference-card">
					<text class="voice-reference-label">建议配音</text>
					<text class="voice-reference-value">{{ referenceVoiceStyle }}</text>
				</view>
				<view class="shot-workspace">
					<view v-if="showSampleSection" class="sample-section">
						<text class="sample-title">样例：</text>
						<view
							v-if="activeShot.sampleVideoUrl"
							class="sample-video-card sample-video-card--ready"
							:class="sampleCardAspectClass(activeShot)"
							@click="previewSampleVideo(activeShot)"
						>
							<view class="media-card__viewport">
								<image
									v-if="activeShot.sampleCoverUrl"
									class="sample-cover"
									:src="activeShot.sampleCoverUrl"
									mode="aspectFit"
								/>
								<view v-else class="sample-cover sample-cover--empty">
									<text class="sample-cover-title">样例视频</text>
								</view>
								<view class="sample-video__mask">
									<text class="sample-play">▶</text>
								</view>
								<text class="sample-aspect-badge">{{ sampleAspectLabel(activeShot) }}</text>
							</view>
							<view class="media-card__footer">
								<text class="media-card__title">参考样例视频</text>
								<text class="media-card__meta">点击播放</text>
							</view>
						</view>
						<view v-else class="sample-video-card sample-video-card--empty">
							<text class="sample-empty-title">暂无样例视频</text>
							<text class="sample-empty-copy">后管上传后会在这里展示</text>
						</view>
					</view>
					<view class="upload-block">
						<view class="upload-block__head">
							<text class="upload-block__title">上传视频</text>
							<text class="upload-block__meta">时长要求 {{ activeShot.durationText }}</text>
						</view>
						<view class="upload-requirements">
							<view class="upload-requirement-row">
								<text class="upload-requirement-label">拍摄要求</text>
								<text class="upload-requirement-value">{{ activeShot.requirement }}</text>
							</view>
							<view class="upload-requirement-row">
								<text class="upload-requirement-label">分镜台词</text>
								<text class="upload-requirement-value">{{ activeShot.voiceover }}</text>
							</view>
						</view>
					</view>
					<view v-if="activeShotVideo" class="shot-preview shot-preview--ready">
						<view class="uploaded-video-cover" @click="previewShotVideo(activeShotIndex)">
							<image v-if="activeShotVideo.poster" class="uploaded-video-poster" :src="activeShotVideo.poster" mode="aspectFill" />
							<view v-else class="uploaded-video-poster uploaded-video-poster--empty"><text>已上传视频</text></view>
							<view class="uploaded-video-mask"><text class="uploaded-video-play">▶</text></view>
							<button class="uploaded-video-delete" @click.stop="removeShotVideo(activeShotIndex)">删除</button>
						</view>
						<view class="media-card__footer uploaded-video-meta">
							<text class="media-card__title video-name">{{ activeShotVideo.name }}</text>
							<text class="media-card__meta video-meta">{{ formatVideoMeta(activeShotVideo) }}</text>
						</view>
					</view>
					<view v-else class="shot-preview" @click="chooseShotVideo(activeShotIndex)">
						<view class="shot-preview__empty">
							<text class="upload-icon">＋</text>
							<text class="upload-title">{{ isOriginalTask ? '上传视频' : `上传 ${activeShot.title} 视频` }}</text>
							<text class="upload-copy">支持拍摄或从相册选择，建议 9:16 竖屏</text>
						</view>
					</view>
				</view>
			</view>

			<view v-if="activeStepKey === 'compose'" class="panel-card">
				<view class="section-head">
					<view>
						<text class="card-title">确认成片方案</text>
					</view>
					<text class="section-count" :class="`section-count--${composeStateTone}`">{{ composeStateText }}</text>
				</view>
				<view v-if="composeBackendMessage" class="compose-status-card" :class="`compose-status-card--${composeStateTone}`">
					<text class="compose-status-title">{{ composeStateText }}</text>
					<text class="compose-status-copy">{{ composeBackendMessage }}</text>
				</view>
				<view class="composed-video-card">
					<text class="compose-section-title">合成视频预览</text>
					<video v-if="composedVideoUrl" class="composed-video-player" :src="composedVideoUrl" :controls="true" :show-progress="true" :show-fullscreen-btn="true" :enable-progress-gesture="true" object-fit="contain" />
					<view v-else class="composed-video-empty">
						<text class="composed-video-empty__title">暂无成片视频</text>
						<text class="composed-video-empty__copy">可先上传本地视频测试预览与下载</text>
					</view>
					<button class="upload-test-video-button" @click="chooseComposedVideo">
						<text>{{ composedVideoUrl ? '更换测试视频' : '上传测试视频' }}</text>
					</button>
					<button class="save-video-button" :disabled="savingVideo" @click="saveComposedVideo">
						<text>{{ savingVideo ? '正在保存…' : '下载视频至相册' }}</text>
					</button>
					<text class="save-video-tip">保存后可直接前往发布平台选择该视频</text>
				</view>
				<view class="compose-section">
					<text class="compose-section-title">1. 配音与字幕</text>
					<view class="compose-config-row"><text>口播方式</text><text class="compose-config-value">分镜口播{{ referenceVoiceStyle ? ` · ${referenceVoiceStyle}` : '' }}</text></view>
					<view class="subtitle-setting">
						<text class="subtitle-setting__label">字幕方式</text>
						<view class="subtitle-mode-tabs">
							<button class="subtitle-mode-button" :class="{ 'subtitle-mode-button--active': draft.subtitleMode !== 'FIXED' }" @click="setSubtitleMode('AUTO')">自动字幕</button>
							<button class="subtitle-mode-button" :class="{ 'subtitle-mode-button--active': draft.subtitleMode === 'FIXED' }" @click="setSubtitleMode('FIXED')">固定字幕</button>
						</view>
					</view>
					<view v-if="draft.subtitleMode === 'FIXED'" class="fixed-subtitle-list">
						<text class="fixed-subtitle-tip">请选择每个分镜的字幕，并确认字幕内容</text>
						<view v-for="(subtitle, index) in draft.fixedSubtitles" :key="`subtitle-${index}`" class="fixed-subtitle-item">
							<view class="fixed-subtitle-head">
								<text class="fixed-subtitle-title">分镜 {{ index + 1 }}</text>
								<button class="state-choice-button" :class="{ 'state-choice-button--active': subtitle.selected === true }" @click="updateFixedSubtitleSelection(index, subtitle.selected !== true)">
									<text class="state-choice-icon">{{ subtitle.selected === true ? '✓' : '' }}</text><text>{{ subtitle.selected === true ? '已选择' : '选择字幕' }}</text>
								</button>
							</view>
							<textarea v-model.trim="subtitle.text" class="fixed-subtitle-input" :disabled="subtitle.selected !== true" :placeholder="`请输入分镜 ${index + 1} 字幕`" @blur="saveDraft"></textarea>
						</view>
					</view>
					<view class="compose-config-row"><text>保留素材原声</text><button class="state-choice-button" :class="{ 'state-choice-button--active': draft.originalAudioEnabled === true }" @click="updateComposeSetting('originalAudioEnabled', draft.originalAudioEnabled !== true)"><text>{{ draft.originalAudioEnabled === true ? '已开启' : '已关闭' }}</text></button></view>
				</view>
				<view class="compose-section">
					<text class="compose-section-title">2. 配乐与画面</text>
					<view class="compose-config-row"><text>背景音乐</text><text class="compose-config-value">{{ referenceBgms[0]?.name || '系统默认配乐' }}</text></view>
					<view class="compose-config-row"><text>节点转场</text><button class="state-choice-button" :class="{ 'state-choice-button--active': draft.transitionsEnabled !== false }" @click="updateComposeSetting('transitionsEnabled', draft.transitionsEnabled === false)"><text>{{ draft.transitionsEnabled !== false ? '已开启' : '已关闭' }}</text></button></view>
					<view class="compose-config-row"><text>封面比例</text><text class="compose-config-value">{{ coverTemplateAspect === 'landscape' ? '横版 16:9' : '竖版 9:16' }}</text></view>
				</view>
				<text class="helper-text">素材已在上一步校验通过。确认设置后，系统将在后台生成成片，离开页面不会中断。</text>
			</view>

			<view v-if="activeStepKey === 'publish'" class="panel-card">
				<view class="section-head">
					<view>
						<text class="card-title">复制话题并发布到平台</text>
					</view>
					<button class="ghost-button ghost-button--mini" @click="copyText(publishTopic, '发布话题')">复制话题</button>
				</view>
				<view class="publish-box">
					<text class="publish-label">推荐标题/话题</text>
					<textarea
						v-model.trim="draft.publishTopic"
						class="publish-topic-input"
						placeholder="请输入发布标题/话题，例如 #中秋 #月饼"
						@blur="saveDraft"></textarea>
					<view class="topic-actions">
						<input
							v-model.trim="topicDraft"
							class="topic-input"
							placeholder="添加话题，如 中秋"
							@confirm="addTopic" />
						<button class="topic-add-button" @click="addTopic">添加</button>
					</view>
				</view>
				<view class="platform-grid">
					<button
						v-for="platform in platformOptions"
						:key="platform"
						class="platform-chip"
						:class="{ 'platform-chip--active': isPlatformSelected(platform) }"
						@click="togglePlatform(platform)"
					>
						{{ platform }}
					</button>
				</view>
				<view class="share-douyin-card">
					<text class="share-douyin-title">分享成片到抖音</text>
					<text class="share-douyin-desc">点击后调起系统分享，选择抖音即可把成片视频带入抖音发布页，无需开放平台权限。</text>
					<button class="share-douyin-button" :disabled="sharingVideo" @click="shareComposedVideo">
						<text>{{ sharingVideo ? '正在准备视频…' : '分享到抖音' }}</text>
					</button>
				</view>
				<view class="notice-box">
					<text class="notice-title">发布动作</text>
					<text class="notice-copy">选择后管指定的平台，点击底部“去发布”拉起对应 App。发布完成后返回本页，再进入回传步骤填写视频链接。</text>
				</view>
			</view>

			<view v-if="activeStepKey === 'callback'" class="panel-card">
				<view class="section-head">
					<view>
						<text class="card-title">填写已发布视频链接</text>
					</view>
				</view>
				<view class="link-list">
					<view v-for="(link, index) in draft.publishLinks" :key="`${link.platform}-${index}`" class="link-card">
						<view class="picker-row">
							<text class="picker-label">发布平台</text>
							<text class="picker-value">{{ link.platform }}</text>
						</view>
						<input
							v-model.trim="link.url"
							class="link-input"
							placeholder="粘贴视频链接，例如 https://..."
							@blur="saveDraft" />
					</view>
				</view>
				<text v-if="draft.linksSaved" class="success-copy">已保存回传草稿：{{ draft.updatedAt }}</text>
			</view>

			<view class="bottom-action">
				<button class="primary-button bottom-action__button" :disabled="primaryActionDisabled" @click="handlePrimaryAction">{{ primaryActionLabel }}</button>
			</view>
		</template>

		<app-tab-bar :active-index="2" @change="changeTab" />
	</view>
</template>

<script>
	import { fetchContentTask } from '@/api/content-tasks.js'
	import { appConfig } from '@/config/app.js'
	import { contentCreationType, formatDeadline, stageTone, taskDisplayStage, taskStatusLabel } from '@/utils/content-task.js'
	import AppTabBar from '@/components/app-tab-bar/app-tab-bar.vue'

	// #ifdef APP-PLUS
	import { shareWithSystem } from '@/uni_modules/tql-share'
	// #endif

	const DEFAULT_DRAFT = Object.freeze({
		videos: [],
		materialValidated: false,
		composeReady: false,
		composedVideoUrl: '',
		captionsEnabled: true,
		subtitleMode: 'AUTO',
		fixedSubtitles: [],
		transitionsEnabled: true,
		originalAudioEnabled: false,
		publishLinks: [{ platform: '抖音', url: '' }],
		publishTopic: '',
		selectedPlatforms: [],
		linksSaved: false,
		lastPlatform: '',
		updatedAt: ''
	})

	export default {
		components: { AppTabBar },
		data() {
			return {
				id: '',
				loading: true,
				errorMessage: '',
				task: null,
				activeStepIndex: 0,
				activeShotIndex: 0,
				showFullInstruction: false,
				bgmPlaying: false,
				activeBgmIndex: -1,
				composing: false,
				savingVideo: false,
				sharingVideo: false,
				pendingShareTempFile: '',
				composePollTimer: null,
				topicDraft: '',
				draft: { ...DEFAULT_DRAFT, videos: [], publishLinks: [{ platform: '抖音', url: '' }] },
				supportedPlatformOptions: ['抖音', '视频号', '小红书', '快手']
			}
		},
		computed: {
			displayStage() { return taskDisplayStage(this.task || {}) },
			displayStatusLabel() { return taskStatusLabel(this.task || {}) },
			taskDraftKey() { return `content-task-draft:${this.id}` },
			deadlineText() { return this.task ? formatDeadline(this.task.deadline) : '' },
			isOverdue() { return this.task?.deadline && new Date(this.task.deadline).getTime() < Date.now() },
			createdText() { return this.task?.createdTime ? new Date(this.task.createdTime).toLocaleString('zh-CN', { hour12: false }) : '—' },
			taskRequirementSections() {
				return this.shotSlots.map((shot, index) => ({
					key: shot.key,
					title: this.isOriginalTask ? '拍摄要求' : `分镜 ${index + 1}`,
					requirement: this.stripStoryboardMetadata(shot.requirement) || '请按后管要求完成本段视频拍摄。',
					voiceover: this.isOriginalTask ? '' : this.stripStoryboardMetadata(shot.voiceover)
				}))
			},
			requirementCopyText() {
				return this.taskRequirementSections
					.map(section => `${section.title}\n${section.requirement}${section.voiceover ? `\n台词：${section.voiceover}` : ''}`)
					.join('\n\n')
			},
			workbenchSteps() {
				const steps = [
					{ key: 'task', title: '任务信息', shortTitle: '任务', desc: '确认计划名称、任务文案、截止时间和状态' },
					{ key: 'upload', title: '视频上传', shortTitle: '上传', desc: '按分镜样例或原创拍摄要求上传竖屏视频' },
					{ key: 'compose', title: '成片设置', shortTitle: '成片', desc: '确认分镜、口播字幕、配乐和转场方案' },
					{ key: 'publish', title: '视频发布', shortTitle: '发布', desc: '复制话题，发布到指定平台' },
					{ key: 'callback', title: '回传链接', shortTitle: '回传', desc: '填写发布链接，完成移动端闭环' }
				]
				return this.isOriginalTask ? steps.filter(step => step.key !== 'compose') : steps
			},
			activeStepKey() { return this.workbenchSteps[this.activeStepIndex]?.key || 'task' },
			isOriginalTask() {
				return contentCreationType(this.task || {}) === 'ORIGINAL'
			},
			showSampleSection() {
				return !this.isOriginalTask
			},
			coverTemplateUrl() {
				const direct = this.task?.coverTemplateUrl || this.task?.videoCoverExampleUrl || ''
				if (direct) return this.resolveAssetUrl(direct)
				const source = this.task?.taskInstruction || ''
				const matched = source.match(/视频封面示例[:：]\s*([^\s\n]+)/)
				return this.resolveAssetUrl(matched ? matched[1].trim() : '')
			},
			coverTemplateAspect() {
				const direct = this.task?.coverTemplateAspect || this.task?.videoCoverAspect || ''
				if (direct) return this.normalizeSampleAspect(direct) || 'portrait'
				const source = this.task?.taskInstruction || ''
				const matched = source.match(/视频封面比例[:：]\s*([^\n]+)/)
				return this.normalizeSampleAspect(matched ? matched[1] : '') || 'portrait'
			},
			referenceBgms() {
				const source = this.task?.taskInstruction || ''
				const items = [...source.matchAll(/参考BGM \d+[:：]\s*([^\n]+)/g)].map(match => {
					const [name, url, category] = match[1].split('｜')
					return { name: name?.trim() || '计划参考配乐', url: this.resolveAssetUrl(url?.trim() || ''), category: category?.trim() || '' }
				}).filter(item => item.url)
				if (items.length) return items
				const url = source.match(/参考BGM地址[:：]\s*([^\s\n]+)/)?.[1]?.trim() || ''
				return url ? [{
					name: source.match(/参考BGM名称[:：]\s*([^\n]+)/)?.[1]?.trim() || '计划参考配乐',
					url: this.resolveAssetUrl(url),
					category: source.match(/参考BGM分类[:：]\s*([^\n]+)/)?.[1]?.trim() || ''
				}] : []
			},
			referenceVoiceStyle() {
				const source = this.task?.taskInstruction || ''
				return source.match(/建议配音[:：]\s*([^\n]+)/)?.[1]?.trim() || ''
			},
			originalRequirementText() {
				const source = this.task?.taskInstruction || ''
				const matched = source.match(/拍摄要求[:：]([\s\S]*)/)
				const value = matched
					? matched[1].split(/\n(?:发布平台|发布标题|发布话题|任务|分镜要求)/)[0].trim()
					: ''
				return value || source || '按后管下发要求完成原创视频拍摄。'
			},
			parsedStoryboardCount() {
				const source = this.task?.taskInstruction || ''
				const matched = source.match(/分镜要求[:：]([\s\S]*)/)
				if (!matched) return 0
				const segment = matched[1].split(/\n(?:发布|拍摄|任务|平台|标题|话题)/)[0].trim()
				const shots = segment.match(/(?:^|[|｜\n]\s*)\d+[.．、]/g)
				return shots ? shots.length : 0
			},
			storyboardCount() {
				if (this.isOriginalTask) return 1
				const parsedCount = this.parsedStoryboardCount
				const apiCount = Number(this.task?.storyboardCount || 0)
				const count = parsedCount || apiCount || 3
				return Math.min(Math.max(count, 1), 8)
			},
			storyboardItems() {
				const source = this.task?.taskInstruction || ''
				if (this.isOriginalTask) {
					return [{
						requirement: this.originalRequirementText,
						voiceover: '原创任务不配置固定分镜台词，请按拍摄要求自主发挥。',
						sampleVideoUrl: '',
						sampleCoverUrl: '',
						durationText: '约8s'
					}]
				}
				const apiItems = Array.isArray(this.task?.storyboards)
					? this.task.storyboards
					: (Array.isArray(this.task?.storyboardSamples) ? this.task.storyboardSamples : [])
				const matched = source.match(/分镜要求[:：]([\s\S]*)/)
				const segment = matched ? matched[1].split(/\n(?:发布平台|发布标题|发布话题|拍摄要求|任务)/)[0].trim() : ''
				const parsedItems = segment
					? segment.split(/\s*[|｜]\s*/).map((item) => this.parseStoryboardText(item))
					: []
				return Array.from({ length: this.storyboardCount }, (_, index) => {
					const apiItem = apiItems[index] || {}
					const parsedItem = parsedItems[index] || {}
					return {
						requirement: apiItem.requirement || apiItem.shootingRequirement || parsedItem.requirement || '',
						voiceover: apiItem.voiceover || apiItem.script || parsedItem.voiceover || '',
						sampleVideoUrl: this.resolveAssetUrl(apiItem.sampleVideoUrl || apiItem.exampleVideoUrl || parsedItem.sampleVideoUrl || ''),
						sampleCoverUrl: this.resolveAssetUrl(apiItem.sampleCoverUrl || apiItem.exampleCoverUrl || parsedItem.sampleCoverUrl || ''),
						sampleAspect: this.normalizeSampleAspect(apiItem.sampleAspect || apiItem.aspect || parsedItem.sampleAspect || ''),
						durationText: apiItem.durationText || apiItem.durationRequirement || parsedItem.durationText || '约8s',
						minDuration: apiItem.minDuration ?? apiItem.minDurationSeconds ?? apiItem.durationMin,
						maxDuration: apiItem.maxDuration ?? apiItem.maxDurationSeconds ?? apiItem.durationMax,
						durationSeconds: apiItem.durationSeconds ?? apiItem.duration
					}
				})
			},
			shotSlots() {
				const items = this.storyboardItems
				const fallback = [
					'开场展示门店或产品，画面保持竖屏稳定，时长建议 3-5 秒。',
					'按任务文案展示核心卖点或服务动作，声音清晰、光线充足。',
					'结尾补充门店氛围、成品特写或顾客体验，便于合成完整短视频。'
				]
				return Array.from({ length: this.storyboardCount }, (_, index) => {
					const item = items[index] || {}
					const durationRule = this.resolveDurationRule(item)
					return {
						key: `shot-${index + 1}`,
						title: this.isOriginalTask ? '视频' : `分镜 ${index + 1}`,
						requirement: item.requirement || fallback[index % fallback.length],
						voiceover: item.voiceover || '根据后管任务文案生成/朗读当前分镜配音，保持自然语速。',
						sampleVideoUrl: item.sampleVideoUrl || '',
						sampleCoverUrl: item.sampleCoverUrl || '',
						sampleAspect: item.sampleAspect || 'portrait',
						durationText: durationRule.label,
						minDuration: durationRule.min,
						maxDuration: durationRule.max
					}
				})
			},
			activeShot() {
				return this.shotSlots[this.activeShotIndex] || this.shotSlots[0]
			},
			activeShotVideo() {
				return this.draft.videos[this.activeShotIndex]
			},
			instructionLines() {
				const source = this.task?.taskInstruction || ''
				const matched = source.match(/分镜要求[:：]([\s\S]*)/)
				if (matched) {
					const segment = matched[1].split(/\n(?:发布|拍摄|任务|平台|标题|话题)/)[0].trim()
					const storyboardLines = segment
						.split(/\s*[|｜]\s*/)
						.map((item) => item.replace(/^\d+[.．、]\s*/, '').trim())
						.filter((item) => item.length >= 2)
					if (storyboardLines.length) return storyboardLines.slice(0, this.storyboardCount)
				}
				return source
					.split(/[\n。；;]/)
					.map((item) => item.trim())
					.filter((item) => item.length >= 6)
					.slice(0, this.storyboardCount)
			},
			uploadedCount() {
				return this.draft.videos.slice(0, this.storyboardCount).filter(Boolean).length
			},
			missingShotCount() {
				return Math.max(0, this.shotSlots.length - this.uploadedCount)
			},
			progressStep() {
				return this.activeStepIndex + 1
			},
			progressPercent() {
				const base = [8, 30, 54, 76, 92][this.activeStepIndex] || 8
				if (this.draft.linksSaved) return 100
				return base
			},
			actionDescription() {
				if (this.task?.stage === 'LOCKED') return '当前任务待解锁，可先阅读任务要求，完成前置训练后再开始拍摄。'
				if (this.task?.stage === 'READY_TO_SHOOT') return this.isOriginalTask ? '查看文案和拍摄要求，上传原创视频。' : '查看文案和拍摄要求，按分镜上传素材。'
				if (this.task?.stage === 'SHOOTING') return this.isOriginalTask ? '完成原创视频上传后，直接进入发布步骤。' : '继续补齐剩余分镜素材，准备合成。'
				if (this.task?.stage === 'PROCESSING') return '系统正在处理作品，可先核对已保存草稿。'
				if (this.task?.stage === 'PENDING_REVIEW') return '作品已提交审核，可保留发布素材，等待审核结果。'
				if (this.task?.stage === 'READY_TO_PUBLISH') return '复制话题发布视频，发布后填写链接回传。'
				if (this.task?.stage === 'COMPLETED') return '任务已完成，后续数据由后台汇总。'
				return '按当前步骤完成后，任务会进入下一阶段。'
			},
			composeStateText() {
				if (this.task?.stage === 'PROCESSING') return '后台合成中'
				if (this.task?.stage === 'PENDING_REVIEW') return '等待审核'
				if (this.draft.composeReady) return '方案已确认'
				if (this.uploadedCount === 0) return '待上传'
				if (this.missingShotCount > 0) return '素材未齐'
				return '待确认'
			},
			composeStateTone() {
				if (this.task?.stage === 'PROCESSING') return 'processing'
				if (this.task?.stage === 'PENDING_REVIEW' || this.draft.composeReady) return 'success'
				if (this.missingShotCount > 0) return 'warning'
				return 'ready'
			},
			composeBackendMessage() {
				if (this.composing) return '视频合成预计3分钟，请耐心等待！'
				if (this.task?.stage === 'PROCESSING') return '系统正在后台拼接分镜、生成字幕并混合配乐，请稍后返回查看。'
				if (this.task?.stage === 'PENDING_REVIEW') return '成片已生成并进入审核，审核通过后即可前往发布。'
				return ''
			},
			composedVideoUrl() {
				const url = this.task?.composedVideoUrl || this.task?.outputVideoUrl || this.task?.resultVideoUrl || this.draft.composedVideoUrl || ''
				return this.resolveAssetUrl(url)
			},
			composeChecks() {
				return [
					{ title: '任务文案', ok: Boolean(this.task?.taskInstruction), copy: this.task?.taskInstruction ? '已读取后管下发文案。' : '后管暂未填写文案。' },
					{ title: '视频素材', ok: this.uploadedCount > 0, copy: this.uploadedCount > 0 ? `已上传 ${this.uploadedCount} 个分镜素材。` : '请至少上传 1 个视频素材。' },
					{ title: '完整分镜', ok: this.missingShotCount === 0, copy: this.missingShotCount === 0 ? '分镜素材已齐全。' : `还缺少 ${this.missingShotCount} 个分镜，可先保存草稿。` }
				]
			},
			defaultPublishTopic() {
				const text = `${this.task?.taskInstruction || ''} ${this.task?.planName || ''}`
				const tags = text.match(/#[\u4e00-\u9fa5A-Za-z0-9_-]+/g)
				if (tags?.length) return [...new Set(tags)].slice(0, 4).join(' ')
				return `#同庆楼 #${this.task?.planName || 'AI短视频'} #门店运营`
			},
			publishTopic() {
				return this.draft.publishTopic || this.defaultPublishTopic
			},
			platformOptions() {
				const direct = this.task?.platforms || this.task?.publishPlatforms || []
				const source = Array.isArray(direct) && direct.length
					? direct
					: ((this.task?.taskInstruction || '').match(/发布平台[:：]\s*([^\n]+)/)?.[1] || '').split(/[、,，|｜/\s]+/)
				const aliases = {
					DOUYIN: '抖音', DOU_YIN: '抖音', WECHAT_CHANNELS: '视频号', WECHAT_VIDEO: '视频号',
					XIAOHONGSHU: '小红书', RED: '小红书', KUAISHOU: '快手'
				}
				const normalized = source
					.map(item => aliases[String(item || '').trim().toUpperCase()] || String(item || '').trim())
					.filter(item => this.supportedPlatformOptions.includes(item))
				return [...new Set(normalized.length ? normalized : ['抖音'])]
			},
			selectedPublishPlatform() {
				return this.platformOptions.includes(this.draft.lastPlatform) ? this.draft.lastPlatform : this.platformOptions[0]
			},
			primaryActionLabel() {
				if (this.activeStepKey === 'task') return '进入视频上传'
				if (this.activeStepKey === 'upload') {
					if (!this.activeShotVideo) return `上传${this.activeShot?.title || '当前分镜'}视频`
					if (this.activeShotIndex < this.shotSlots.length - 1) return '下一个分镜'
					return this.isOriginalTask ? '去发布视频' : '合成视频'
				}
				if (this.activeStepKey === 'compose') {
					if (this.composing) return '视频合成中'
					if (this.task?.stage === 'PROCESSING') return '后台合成中'
					if (this.task?.stage === 'PENDING_REVIEW') return '等待审核'
					return '去发布视频'
				}
				if (this.activeStepKey === 'publish') return '去发布'
				return '保存回传信息'
			},
			primaryActionDisabled() {
				return this.activeStepKey === 'compose' && (this.composing || ['PROCESSING', 'PENDING_REVIEW'].includes(this.task?.stage))
			}
		},
		onLoad(options) {
			this.id = options.id || ''
			this.load()
		},
		onShow() {
			if (this.id) {
				this.loadDraft()
				if (this.task?.stage === 'PROCESSING') this.refreshComposeState()
			}
		},
		onUnload() {
			this.stopComposePolling()
			this.destroyReferenceBgm()
		},
		methods: {
			saveVideoFileToAlbum(filePath) {
				return new Promise((resolve, reject) => {
					const nativePlus = typeof plus !== 'undefined' ? plus : null
					if (nativePlus?.gallery?.save) {
						let nativePath = filePath
						try {
							if (!/^file:\/\//i.test(nativePath) && nativePlus.io?.convertLocalFileSystemURL) {
								nativePath = nativePlus.io.convertLocalFileSystemURL(nativePath)
							}
						} catch (error) {
							reject(error)
							return
						}
						nativePlus.gallery.save(nativePath, resolve, reject)
						return
					}
					uni.saveVideoToPhotosAlbum({
						filePath,
						success: resolve,
						fail: reject
					})
				})
			},
			chooseComposedVideo() {
				uni.chooseVideo({
					count: 1,
					compressed: false,
					sourceType: ['album'],
					success: (result) => {
						const localPath = result.tempFilePath || result.tempFile || ''
						if (!localPath) {
							uni.showToast({ title: '未获取到视频文件', icon: 'none' })
							return
						}
						this.draft = { ...this.draft, composedVideoUrl: localPath, composeReady: true }
						this.saveDraft()
						uni.showToast({ title: '测试视频已添加', icon: 'success' })
					},
					fail: (error) => {
						if (!/cancel/i.test(String(error?.errMsg || ''))) uni.showToast({ title: '视频选择失败', icon: 'none' })
					}
				})
			},
			async saveComposedVideo() {
				if (this.savingVideo) return
				if (!this.composedVideoUrl) {
					uni.showToast({ title: '暂无可下载的成片视频', icon: 'none' })
					return
				}
				this.savingVideo = true
				uni.showLoading({ title: '正在下载视频', mask: true })
				let temporaryFilePath = this.composedVideoUrl
				try {
					if (/^https?:\/\//i.test(this.composedVideoUrl)) {
						const downloadResult = await new Promise((resolve, reject) => {
							uni.downloadFile({
								url: this.composedVideoUrl,
								success: (result) => result.statusCode >= 200 && result.statusCode < 300
									? resolve(result)
									: reject(new Error(`视频下载失败（${result.statusCode}）`)),
								fail: reject
							})
						})
						temporaryFilePath = downloadResult.tempFilePath
					}
					await this.saveVideoFileToAlbum(temporaryFilePath)
					uni.showToast({ title: '已保存到相册', icon: 'success' })
				} catch (error) {
					const message = String(error?.errMsg || error?.message || '')
					const denied = /auth deny|authorize|permission|权限/i.test(message)
					uni.showModal({
						title: denied ? '需要相册权限' : '保存失败',
						content: denied ? '请在系统设置中允许访问照片或相册后重试。' : (message || '视频暂时无法保存，请稍后重试。'),
						showCancel: false
					})
				} finally {
					uni.hideLoading()
					this.savingVideo = false
				}
			},
			async refreshComposeState() {
				try {
					this.task = await fetchContentTask(this.id)
					if (this.composedVideoUrl) {
						this.draft = { ...this.draft, composeReady: true, composedVideoUrl: this.composedVideoUrl }
						this.saveDraft()
						this.stopComposePolling()
					} else if (this.task?.stage === 'PROCESSING') {
						this.scheduleComposePolling()
					}
				} catch (_) {
					this.scheduleComposePolling()
				}
			},
			scheduleComposePolling() {
				this.stopComposePolling()
				this.composePollTimer = setTimeout(() => this.refreshComposeState(), 10000)
			},
			stopComposePolling() {
				if (this.composePollTimer) clearTimeout(this.composePollTimer)
				this.composePollTimer = null
			},
			selectWorkbenchStep(index) {
				const targetStep = this.workbenchSteps[index]
				if (!targetStep) return
				if (targetStep.key === 'compose' && !this.draft.materialValidated && !['PROCESSING', 'PENDING_REVIEW'].includes(this.task?.stage)) {
					uni.showToast({ title: '请先在上传页检查并合成', icon: 'none' })
					return
				}
				this.activeStepIndex = index
			},
			stripStoryboardMetadata(value) {
				return String(value || '')
					.replace(/(?:样例|示例)(?:视频|封面)(?:链接|地址)?\s*[:：]?\s*(?:https?:\/\/|\/api\/)[^\s；;|｜，,]+/gi, '')
					.replace(/https?:\/\/[^\s；;|｜，,]+/gi, '')
					.replace(/(?:样例|示例)(?:比例|画幅)\s*[:：]?\s*[^；;|｜，,。]+/gi, '')
					.replace(/\s*[；;|｜]\s*([；;|｜]\s*)+/g, '；')
					.replace(/^[\s；;|｜，,]+|[\s；;|｜，,]+$/g, '')
					.trim()
			},
			resolveAssetUrl(url) {
				if (!url) return ''
				if (/^(?:_doc|_www|file:|blob:|content:|wxfile:|ttfile:)/i.test(String(url))) return url
				if (/^https?:\/\//i.test(url)) return url
				return `${appConfig.apiBaseUrl.replace(/\/$/, '')}/${String(url).replace(/^\//, '')}`
			},
			previewCoverTemplate() {
				if (!this.coverTemplateUrl) return
				uni.previewImage({ current: this.coverTemplateUrl, urls: [this.coverTemplateUrl] })
			},
			toggleReferenceBgm(bgm, index) {
				if (!bgm?.url) return
				if (this._bgmAudioContext && this.activeBgmIndex !== index) this.destroyReferenceBgm()
				if (!this._bgmAudioContext) {
					const audioContext = uni.createInnerAudioContext()
					audioContext.src = bgm.url
					this.activeBgmIndex = index
					audioContext.onPlay(() => { this.bgmPlaying = true })
					audioContext.onPause(() => { this.bgmPlaying = false })
					audioContext.onStop(() => { this.bgmPlaying = false })
					audioContext.onEnded(() => { this.bgmPlaying = false })
					audioContext.onError(() => {
						this.bgmPlaying = false
						uni.showToast({ title: '参考 BGM 播放失败', icon: 'none' })
					})
					this._bgmAudioContext = audioContext
				}
				if (this.bgmPlaying) this._bgmAudioContext.pause()
				else this._bgmAudioContext.play()
			},
			destroyReferenceBgm() {
				if (this._bgmAudioContext) this._bgmAudioContext.destroy()
				this._bgmAudioContext = null
				this.bgmPlaying = false
				this.activeBgmIndex = -1
			},
			parseStoryboardText(rawText) {
				const text = String(rawText || '').replace(/^\d+[.．、]\s*/, '').trim()
				const pick = (label) => {
					const match = text.match(new RegExp(`${label}[:：]\\s*([^；;|｜]+)`))
					return match ? match[1] : ''
				}
				const scriptMatch = text.match(/(?:台词|配音文案)[:：]([^；;|｜]+)/)
				const durationMatch = text.match(/(?:时长要求|时长)[:：]?\s*([^；;|｜]+)/)
				return {
					requirement: text
						.replace(/(?:台词|配音文案)[:：][^；;|｜]+/g, '')
						.replace(/(?:样例视频|样例封面|示例视频|示例封面|样例比例|视频比例)[:：]\s*[^；;|｜]+/g, '')
						.replace(/(?:时长要求|时长)[:：]?\s*[^；;|｜]+/g, '')
						.replace(/[；;|｜]+$/g, '')
						.trim(),
					voiceover: scriptMatch ? scriptMatch[1].trim() : '',
					sampleVideoUrl: pick('样例视频') || pick('示例视频'),
					sampleCoverUrl: pick('样例封面') || pick('示例封面'),
					sampleAspect: this.normalizeSampleAspect(pick('样例比例') || pick('视频比例')),
					durationText: durationMatch ? durationMatch[1].trim() : ''
				}
			},
			resolveDurationRule(item = {}) {
				const configuredMin = Number(item.minDuration)
				const configuredMax = Number(item.maxDuration)
				const configuredExact = Number(item.durationSeconds)
				if (configuredMin > 0 || configuredMax > 0) {
					const min = configuredMin > 0 ? configuredMin : configuredMax
					const max = configuredMax > 0 ? configuredMax : configuredMin
					return { min, max, label: min === max ? `${min}秒` : `${min}-${max}秒` }
				}
				if (configuredExact > 0) {
					return { min: Math.max(0.1, configuredExact - 0.5), max: configuredExact + 0.5, label: `${configuredExact}秒` }
				}
				const text = String(item.durationText || '').trim()
				const range = text.match(/(\d+(?:\.\d+)?)\s*(?:秒|s)?\s*[-~～至到]\s*(\d+(?:\.\d+)?)/i)
				if (range) {
					const min = Number(range[1])
					const max = Number(range[2])
					return { min: Math.min(min, max), max: Math.max(min, max), label: text || `${min}-${max}秒` }
				}
				const exact = text.match(/(\d+(?:\.\d+)?)/)
				if (exact) {
					const seconds = Number(exact[1])
					const tolerance = /约|左右|大约/.test(text) ? 1 : 0.5
					return { min: Math.max(0.1, seconds - tolerance), max: seconds + tolerance, label: text || `${seconds}秒` }
				}
				return { min: 0, max: 90, label: text || '以后台设置为准' }
			},
			validateVideoDuration(video, shot) {
				const duration = Number(video?.duration || 0)
				if (!(duration > 0)) return { valid: false, message: '无法读取视频时长，请重新选择视频。' }
				const min = Number(shot?.minDuration || 0)
				const max = Number(shot?.maxDuration || 0)
				if ((min > 0 && duration < min) || (max > 0 && duration > max)) {
					return {
						valid: false,
						message: `${shot?.title || '当前分镜'}要求${shot?.durationText || '符合后台时长设置'}，所选视频为${duration.toFixed(1)}秒。`
					}
				}
				return { valid: true, message: '' }
			},
			normalizeSampleAspect(value) {
				const text = String(value || '').toLowerCase()
				if (/16\s*[:：]\s*9|landscape|horizontal|横/.test(text)) return 'landscape'
				if (/9\s*[:：]\s*16|portrait|vertical|竖/.test(text)) return 'portrait'
				return ''
			},
			sampleAspectLabel(shot) {
				return shot?.sampleAspect === 'landscape' ? '横版 16:9' : '竖版 9:16'
			},
			sampleCardAspectClass(shot) {
				return shot?.sampleAspect === 'landscape' ? 'sample-video-card--landscape' : 'sample-video-card--portrait'
			},
			async load() {
				if (!this.id) { this.loading = false; this.errorMessage = '任务参数无效'; return }
				this.loading = true
				this.errorMessage = ''
				try {
					this.task = await fetchContentTask(this.id)
					this.loadDraft()
					this.activeStepIndex = this.initialStepIndex(this.task?.stage)
					this.activeShotIndex = this.firstUnfinishedShotIndex()
				} catch (error) {
					this.errorMessage = error instanceof Error ? error.message : '请稍后重试'
				} finally {
					this.loading = false
				}
			},
			initialStepIndex(stage) {
				const stageKeys = {
					LOCKED: 'task', READY_TO_SHOOT: 'upload', SHOOTING: 'upload', NEEDS_REVISION: 'upload',
					PROCESSING: this.isOriginalTask ? 'publish' : 'compose',
					PENDING_REVIEW: this.isOriginalTask ? 'publish' : 'compose',
					READY_TO_PUBLISH: 'publish', COMPLETED: 'callback'
				}
				const index = this.workbenchSteps.findIndex(step => step.key === (stageKeys[stage] || 'task'))
				return index < 0 ? 0 : index
			},
			loadDraft() {
				if (!this.id) return
				const cached = uni.getStorageSync(this.taskDraftKey)
				if (!cached) {
					this.draft = { ...DEFAULT_DRAFT, videos: [], publishTopic: this.defaultPublishTopic, selectedPlatforms: [...this.platformOptions], lastPlatform: this.platformOptions[0], publishLinks: this.platformOptions.map(platform => ({ platform, url: '' })) }
					return
				}
				const selectedPlatforms = [...this.platformOptions]
				const cachedLinks = Array.isArray(cached.publishLinks) ? cached.publishLinks : []
				this.draft = {
					...DEFAULT_DRAFT,
					...cached,
					videos: Array.isArray(cached.videos) ? cached.videos.slice(0, this.storyboardCount) : [],
					publishTopic: cached.publishTopic || this.defaultPublishTopic,
					selectedPlatforms,
					lastPlatform: this.platformOptions.includes(cached.lastPlatform) ? cached.lastPlatform : this.platformOptions[0],
					publishLinks: this.platformOptions.map(platform => cachedLinks.find(link => link.platform === platform) || { platform, url: '' })
				}
			},
			saveDraft() {
				this.draft = { ...this.draft, updatedAt: new Date().toLocaleString('zh-CN', { hour12: false }) }
				uni.setStorageSync(this.taskDraftKey, this.draft)
			},
			handlePrimaryAction() {
				if (this.activeStepKey === 'task') {
					this.activeStepIndex = 1
					return
				}
				if (this.activeStepKey === 'upload') {
					if (!this.activeShotVideo) this.chooseShotVideo(this.activeShotIndex)
					else if (this.activeShotIndex < this.shotSlots.length - 1) this.goToNextShot()
					else if (this.isOriginalTask) this.activeStepIndex = this.workbenchSteps.findIndex(step => step.key === 'publish')
					else this.checkMaterialsAndEnterCompose()
					return
				}
				if (this.activeStepKey === 'compose') {
					this.activeStepIndex = this.workbenchSteps.findIndex(step => step.key === 'publish')
					return
				}
				if (this.activeStepKey === 'publish') {
					this.launchPublishPlatform(this.selectedPublishPlatform)
					return
				}
				this.savePublishLinks()
			},
			chooseShotVideo(index) {
				const shot = this.shotSlots[index]
				uni.chooseVideo({
					count: 1,
					compressed: true,
					maxDuration: Math.min(90, Math.max(1, Math.ceil(shot?.maxDuration || 90))),
					sourceType: ['album', 'camera'],
					success: (res) => {
						const selectedVideo = {
							name: `分镜${index + 1}视频`,
							path: res.tempFilePath,
							poster: res.thumbTempFilePath || '',
							duration: Number(res.duration || 0),
							size: Number(res.size || 0),
							width: Number(res.width || 0),
							height: Number(res.height || 0)
						}
						const validation = this.validateVideoDuration(selectedVideo, shot)
						if (!validation.valid) {
							uni.showModal({ title: '视频时长不符合要求', content: validation.message, showCancel: false })
							return
						}
						const videos = [...this.draft.videos]
						videos[index] = selectedVideo
						this.draft = { ...this.draft, videos, materialValidated: false, composeReady: false }
						this.saveDraft()
						uni.showToast({ title: '视频已保存到草稿', icon: 'success' })
					}
				})
			},
			goToNextShot() {
				if (this.activeShotIndex >= this.shotSlots.length - 1) return
				this.activeShotIndex += 1
				this.$nextTick(() => {
					uni.pageScrollTo({ selector: '.shot-switcher', duration: 220 })
				})
			},
			firstUnfinishedShotIndex() {
				for (let index = 0; index < this.shotSlots.length; index += 1) {
					if (!this.draft.videos[index]) return index
				}
				return 0
			},
			switchShot(offset) {
				const nextIndex = this.activeShotIndex + offset
				if (nextIndex < 0 || nextIndex >= this.shotSlots.length) return
				this.activeShotIndex = nextIndex
			},
			previewShotVideo(index) {
				const video = this.draft.videos[index]
				if (!video?.path) return
				const src = encodeURIComponent(video.path)
				const poster = encodeURIComponent(video.poster || '')
				const title = encodeURIComponent(video.name || `分镜${index + 1}视频`)
				const aspect = video.width > video.height ? 'landscape' : 'portrait'
				uni.navigateTo({ url: `/pages/content-tasks/player?src=${src}&poster=${poster}&title=${title}&aspect=${aspect}` })
			},
			previewSampleVideo(shot) {
				if (!shot?.sampleVideoUrl) {
					uni.showToast({ title: '暂无样例视频', icon: 'none' })
					return
				}
				const src = encodeURIComponent(shot.sampleVideoUrl)
				const poster = encodeURIComponent(shot.sampleCoverUrl || '')
				const title = encodeURIComponent(shot.title || '样例视频')
				const aspect = encodeURIComponent(shot.sampleAspect || 'portrait')
				uni.navigateTo({ url: `/pages/content-tasks/player?src=${src}&poster=${poster}&title=${title}&aspect=${aspect}` })
			},
			removeShotVideo(index) {
				const videos = [...this.draft.videos]
				videos[index] = null
				this.draft = { ...this.draft, videos, materialValidated: false, composeReady: false }
				this.saveDraft()
			},
			checkMaterialsAndEnterCompose() {
				if (this.missingShotCount > 0) {
					this.activeShotIndex = this.firstUnfinishedShotIndex()
					uni.showToast({ title: `还缺少 ${this.missingShotCount} 个分镜素材`, icon: 'none' })
					return
				}
				const invalidIndex = this.shotSlots.findIndex((shot, index) => !this.validateVideoDuration(this.draft.videos[index], shot).valid)
				if (invalidIndex >= 0) {
					this.activeShotIndex = invalidIndex
					const validation = this.validateVideoDuration(this.draft.videos[invalidIndex], this.shotSlots[invalidIndex])
					uni.showModal({ title: '视频时长不符合要求', content: validation.message, showCancel: false })
					return
				}
				this.draft = { ...this.draft, materialValidated: true, composeReady: false }
				this.saveDraft()
				this.activeStepIndex = this.workbenchSteps.findIndex(step => step.key === 'compose')
				uni.showToast({ title: '素材校验通过', icon: 'success' })
			},
			updateComposeSetting(key, value) {
				this.draft = { ...this.draft, [key]: value, composeReady: false, composedVideoUrl: '' }
				this.saveDraft()
			},
			ensureFixedSubtitles() {
				const current = Array.isArray(this.draft.fixedSubtitles) ? this.draft.fixedSubtitles : []
				const fixedSubtitles = this.shotSlots.map((shot, index) => ({
					selected: current[index]?.selected === true,
					text: current[index]?.text ?? shot.voiceover ?? ''
				}))
				this.draft = { ...this.draft, fixedSubtitles }
			},
			setSubtitleMode(mode) {
				if (mode === 'FIXED') this.ensureFixedSubtitles()
				this.draft = { ...this.draft, subtitleMode: mode === 'FIXED' ? 'FIXED' : 'AUTO', composeReady: false, composedVideoUrl: '' }
				this.saveDraft()
			},
			updateFixedSubtitleSelection(index, selected) {
				this.ensureFixedSubtitles()
				const fixedSubtitles = this.draft.fixedSubtitles.map((item, itemIndex) => itemIndex === index ? { ...item, selected } : item)
				this.draft = { ...this.draft, fixedSubtitles, composeReady: false, composedVideoUrl: '' }
				this.saveDraft()
			},
			copyText(value, label) {
				if (!value) {
					uni.showToast({ title: `${label}为空`, icon: 'none' })
					return
				}
				uni.setClipboardData({ data: value, success: () => uni.showToast({ title: `已复制${label}`, icon: 'success' }) })
			},
			addTopic() {
				const rawTopic = this.topicDraft.trim()
				if (!rawTopic) {
					uni.showToast({ title: '请输入话题', icon: 'none' })
					return
				}
				const topic = rawTopic.startsWith('#') ? rawTopic : `#${rawTopic}`
				const current = this.draft.publishTopic || this.defaultPublishTopic
				const topics = current.split(/\s+/).filter(Boolean)
				if (!topics.includes(topic)) topics.push(topic)
				this.draft = { ...this.draft, publishTopic: topics.join(' ') }
				this.topicDraft = ''
				this.saveDraft()
			},
			isPlatformSelected(platform) {
				return this.selectedPublishPlatform === platform
			},
			togglePlatform(platform) {
				if (!this.platformOptions.includes(platform)) return
				this.draft = { ...this.draft, selectedPlatforms: [...this.platformOptions], lastPlatform: platform }
				this.saveDraft()
			},
			launchPublishPlatform(platform) {
				const applications = {
					抖音: { pname: 'com.ss.android.ugc.aweme', action: 'snssdk1128://' },
					视频号: { pname: 'com.tencent.mm', action: 'weixin://' },
					小红书: { pname: 'com.xingin.xhs', action: 'xhsdiscover://' },
					快手: { pname: 'com.smile.gifmaker', action: 'kwai://' }
				}
				const application = applications[platform]
				if (!application) {
					uni.showToast({ title: '暂不支持该发布平台', icon: 'none' })
					return
				}
				if (typeof plus === 'undefined' || !plus.runtime?.launchApplication) {
					uni.showToast({ title: '请在手机 App 中使用去发布', icon: 'none' })
					return
				}
				plus.runtime.launchApplication(application, () => {}, () => {
					uni.showModal({
						title: `未能打开${platform}`,
						content: `请确认手机已安装${platform}，安装后再试。`,
						showCancel: false
					})
				})
			},
			normalizeShareFilePath(filePath) {
				if (/^file:\/\//i.test(filePath)) {
					return filePath.replace(/^file:\/\//i, '')
				}
				if (typeof plus !== 'undefined' && plus.io?.convertLocalFileSystemURL) {
					try {
						return plus.io.convertLocalFileSystemURL(filePath)
					} catch (error) {
						return filePath
					}
				}
				return filePath
			},
			ensureVideoExtension(filePath) {
				return new Promise((resolve, reject) => {
					if (/\.(mp4|mov|m4v|avi|3gp|webm)$/i.test(filePath)) {
						resolve(filePath)
						return
					}
					if (typeof plus === 'undefined' || !plus.io?.resolveLocalFileSystemURL) {
						resolve(filePath)
						return
					}
					try {
						const dirPath = plus.io.convertLocalFileSystemURL('_doc/')
						const targetName = `tql_share_${Date.now()}.mp4`
						plus.io.resolveLocalFileSystemURL(dirPath, (dirEntry) => {
							plus.io.resolveLocalFileSystemURL(filePath, (sourceEntry) => {
								sourceEntry.copyTo(dirEntry, targetName, (newEntry) => {
									const copiedPath = this.normalizeShareFilePath(newEntry.toLocalURL())
									this.pendingShareTempFile = copiedPath
									resolve(copiedPath)
								}, reject)
							}, reject)
						}, reject)
					} catch (error) {
						resolve(filePath)
					}
				})
			},
			removeShareTempFile() {
				const tempPath = this.pendingShareTempFile
				this.pendingShareTempFile = ''
				if (!tempPath || typeof plus === 'undefined' || !plus.io?.resolveLocalFileSystemURL) {
					return
				}
				// 延迟删除：等接收方（如抖音）完成读取后再清理临时副本
				setTimeout(() => {
					plus.io.resolveLocalFileSystemURL(tempPath, (entry) => {
						entry.remove(
							() => console.log('[tql-share] 已清理临时分享文件:', tempPath),
							() => {}
						)
					}, () => {})
				}, 60000)
			},
			async shareComposedVideo() {
				if (this.sharingVideo) return
				// 优先分享用户本地上传/选择的测试视频，其次才是后端的成片视频
				const localPath = this.draft.composedVideoUrl || ''
				const shareSource = localPath || this.composedVideoUrl
				if (!shareSource) {
					uni.showToast({ title: '暂无可分享的成片视频', icon: 'none' })
					return
				}
				if (typeof plus === 'undefined' || typeof shareWithSystem === 'undefined') {
					uni.showToast({ title: '请在手机 App 中使用分享到抖音', icon: 'none' })
					return
				}
				this.sharingVideo = true
				this.pendingShareTempFile = ''
				uni.showLoading({ title: '正在准备视频', mask: true })
				try {
					let filePath = shareSource
					if (/^https?:\/\//i.test(filePath)) {
						const downloadResult = await new Promise((resolve, reject) => {
							uni.downloadFile({
								url: filePath,
								success: (result) => result.statusCode >= 200 && result.statusCode < 300
									? resolve(result)
									: reject(new Error(`视频下载失败（${result.statusCode}）`)),
								fail: reject
							})
						})
						filePath = downloadResult.tempFilePath
					}
					let absolutePath = this.normalizeShareFilePath(filePath)
					absolutePath = await this.ensureVideoExtension(absolutePath)
					console.log('[tql-share] 分享视频路径:', absolutePath)
					shareWithSystem({
						type: 'video',
						path: absolutePath,
						success: () => uni.showToast({ title: '已调起分享面板', icon: 'success' }),
						fail: (error) => {
							const message = String(error?.errMsg || error?.message || '')
							if (/cancel/i.test(message)) return
							uni.showModal({
								title: '分享失败',
								content: message || '请确认手机已安装可接收视频的应用（如抖音）后重试。',
								showCancel: false
							})
						}
					})
				} catch (error) {
					uni.showModal({
						title: '分享失败',
						content: String(error?.errMsg || error?.message || '视频暂时无法分享，请稍后重试。'),
						showCancel: false
					})
				} finally {
					uni.hideLoading()
					this.sharingVideo = false
					this.removeShareTempFile()
				}
			},
			savePublishLinks() {
				const validLinks = this.draft.publishLinks.filter((item) => /^https?:\/\//i.test(item.url || ''))
				if (!validLinks.length) {
					uni.showToast({ title: '请填写至少 1 个有效链接', icon: 'none' })
					return
				}
				this.draft = { ...this.draft, linksSaved: true, publishLinks: this.draft.publishLinks }
				this.saveDraft()
				uni.showModal({
					title: '已保存回传信息',
					content: '链接已保存到本机任务草稿。后端开放提交接口后，可直接从这里提交审核。',
					showCancel: false
				})
			},
			formatVideoMeta(video) {
				const seconds = video.duration ? `${Math.round(video.duration)}秒` : '时长未知'
				const size = video.size ? `${(video.size / 1024 / 1024).toFixed(1)}MB` : '大小未知'
				return `${seconds} · ${size}`
			},
			goBack() { uni.navigateBack() },
			changeTab(index) {
				if (index === 2) {
					uni.redirectTo({ url: '/pages/content-tasks/index' })
					return
				}
				uni.reLaunch({ url: `/pages/home/index?tab=${index}` })
			},
			tone: stageTone
		}
	}
</script>

<style scoped>
	.detail-page { box-sizing: border-box; min-height: 100vh; padding: calc(env(safe-area-inset-top) + 128rpx) 32rpx calc(248rpx + env(safe-area-inset-bottom)); background: #f5f6f7; color: #1d2129; font-family: "PingFang SC", "Microsoft YaHei", system-ui, sans-serif; }
	button::after { border: 0; }
	button, .primary-button, .ghost-button, .step-tab, .step-tab__title, .shot-pill, .platform-chip, .text-button, .stage-tag { white-space: nowrap; }
	button[disabled] { opacity: .55; }
	.page-top { position: fixed; top: 0; right: 0; left: 0; z-index: 60; box-sizing: border-box; display: flex; align-items: center; min-height: calc(env(safe-area-inset-top) + 104rpx); padding: calc(env(safe-area-inset-top) + 18rpx) 32rpx 18rpx; background: rgba(245,246,247,.98); backdrop-filter: blur(18rpx); -webkit-backdrop-filter: blur(18rpx); }
	.back-button { flex: 0 0 auto; width: 64rpx; height: 64rpx; margin: 0 16rpx 0 0; padding: 0; border: 0; border-radius: 12rpx; background: #fff; color: #1d2129; font-size: 48rpx; line-height: 58rpx; }
	.page-heading { min-width: 0; }
	.page-title { display: block; font-size: 36rpx; font-weight: 650; }
	.page-subtitle { display: block; margin-top: 4rpx; color: #86909c; font-size: 22rpx; }
	.hero-card { position: relative; margin-top: 10rpx; padding: 18rpx 28rpx 22rpx; border-radius: 16rpx; background: #165dff; color: #fff; }
	.hero-top, .section-head, .shot-card__top, .progress-meta, .picker-row { display: flex; align-items: center; justify-content: space-between; }
	.hero-top { position: absolute; top: 20rpx; right: 28rpx; justify-content: flex-end; }
	.activity-name { color: rgba(255,255,255,.76); font-size: 22rpx; }
	.stage-tag { padding: 8rpx 12rpx; border-radius: 8rpx; background: rgba(255,255,255,.18); font-size: 20rpx; }
	.task-name { display: block; max-width: 510rpx; margin-top: 0; padding-top: 0; font-size: 34rpx; font-weight: 650; line-height: 1.32; }
	.task-code { display: block; margin-top: 8rpx; color: rgba(255,255,255,.68); font-size: 22rpx; }
	.deadline { display: block; margin-top: 6rpx; color: rgba(255,255,255,.84); font-size: 24rpx; }
	.deadline--danger { color: #fff2c7; font-weight: 600; }
	.progress-meta { justify-content: flex-end; margin-top: 10rpx; color: rgba(255,255,255,.76); font-size: 22rpx; }
	.step-scroll { position: sticky; top: calc(env(safe-area-inset-top) + 104rpx); z-index: 50; width: auto; margin: 18rpx -32rpx 0; padding: 14rpx 32rpx 12rpx; background: rgba(245,246,247,.96); white-space: nowrap; backdrop-filter: blur(18rpx); -webkit-backdrop-filter: blur(18rpx); }
	.step-tabs { display: inline-flex; gap: 12rpx; min-width: 100%; padding-bottom: 4rpx; }
	.step-tab { display: inline-flex; flex: 0 0 auto; align-items: center; min-width: 124rpx; height: 58rpx; margin: 0; padding: 0 16rpx; overflow: hidden; border: 1rpx solid #e5e6eb; border-radius: 14rpx; background: #fff; color: #4e5969; line-height: 58rpx; }
	.step-tab--active { border-color: #165dff; background: #e8f3ff; color: #165dff; }
	.step-tab__index { display: inline-flex; align-items: center; justify-content: center; width: 32rpx; height: 32rpx; margin-right: 8rpx; border-radius: 16rpx; background: #f2f3f5; color: inherit; font-size: 18rpx; font-weight: 650; line-height: 32rpx; }
	.step-tab--active .step-tab__index { background: #165dff; color: #fff; }
	.step-tab__title { overflow: hidden; font-size: 23rpx; font-weight: 600; text-overflow: ellipsis; }
	.panel-card, .state-card { margin-top: 18rpx; padding: 28rpx; border: 1rpx solid #e5e6eb; border-radius: 16rpx; background: #fff; }
	.card-kicker { display: block; color: #165dff; font-size: 22rpx; font-weight: 600; }
	.card-title, .state-title { display: block; margin-top: 0; font-size: 32rpx; font-weight: 650; line-height: 1.35; }
	.requirement-list { margin-top: 22rpx; }
	.requirement-section { padding: 24rpx 0; border-top: 1rpx solid #f2f3f5; }
	.requirement-section:first-child { padding-top: 4rpx; border-top: 0; }
	.requirement-section:last-child { padding-bottom: 0; }
	.requirement-section__head { display: flex; align-items: center; }
	.requirement-index { display: flex; align-items: center; justify-content: center; width: 36rpx; height: 36rpx; border-radius: 50%; background: #e8f0ff; color: #165dff; font-size: 22rpx; font-weight: 700; }
	.requirement-title { margin-left: 12rpx; color: #1d2129; font-size: 27rpx; font-weight: 650; }
	.requirement-copy { display: block; margin-top: 14rpx; color: #4e5969; font-size: 26rpx; line-height: 1.75; text-align: justify; }
	.requirement-voiceover { display: flex; align-items: flex-start; margin-top: 16rpx; padding: 18rpx 20rpx; border-radius: 12rpx; background: #f7f8fa; }
	.requirement-label { flex: 0 0 auto; margin-right: 14rpx; color: #165dff; font-size: 23rpx; font-weight: 650; }
	.requirement-voiceover__copy { flex: 1; color: #4e5969; font-size: 24rpx; line-height: 1.65; }
	.text-button { width: auto; height: 54rpx; margin: 12rpx 0 0; padding: 0; border: 0; background: transparent; color: #165dff; font-size: 24rpx; line-height: 54rpx; text-align: left; }
	.detail-grid { display: flex; flex-wrap: wrap; margin-top: 24rpx; padding-top: 24rpx; border-top: 1rpx solid #f2f3f5; }
	.detail-item { box-sizing: border-box; width: 50%; padding: 0 16rpx 20rpx 0; }
	.detail-label { display: block; color: #86909c; font-size: 22rpx; }
	.detail-value { display: block; margin-top: 8rpx; color: #1d2129; font-size: 24rpx; line-height: 1.45; }
	.notice-box { margin-top: 24rpx; padding: 24rpx; border-radius: 16rpx; background: #f2f7ff; }
	.notice-title { display: block; color: #165dff; font-size: 24rpx; font-weight: 650; }
	.notice-copy, .helper-text, .state-copy { display: block; margin-top: 10rpx; color: #4e5969; font-size: 24rpx; line-height: 1.6; }
	.section-count { color: #165dff; font-size: 28rpx; font-weight: 650; }
	.section-count--processing { color: #ff7d00; }
	.section-count--success { color: #00b42a; }
	.section-count--warning { color: #f53f3f; }
	.shot-switcher { position: sticky; top: calc(env(safe-area-inset-top) + 186rpx); z-index: 45; width: auto; margin: 22rpx -28rpx 0; padding: 12rpx 28rpx; background: rgba(255,255,255,.96); white-space: nowrap; backdrop-filter: blur(16rpx); -webkit-backdrop-filter: blur(16rpx); }
	.shot-switcher__inner { display: inline-flex; gap: 12rpx; min-width: 100%; }
	.shot-pill { display: inline-flex; align-items: center; width: auto; height: 60rpx; margin: 0; padding: 0 18rpx; border: 1rpx solid #e5e6eb; border-radius: 30rpx; background: #fff; color: #4e5969; font-size: 23rpx; line-height: 60rpx; }
	.shot-pill--done { border-color: #e5e6eb; background: #fff; color: #4e5969; font-weight: 400; }
	.shot-pill--active { border-color: #ff7d00; background: #fff7e8; color: #ff7d00; font-weight: 650; }
	.shot-pill__dot { display: inline-flex; align-items: center; justify-content: center; width: 30rpx; height: 30rpx; margin-right: 8rpx; border-radius: 15rpx; background: #f2f3f5; color: inherit; font-size: 18rpx; font-weight: 700; line-height: 30rpx; }
	.shot-pill--done .shot-pill__dot { background: #f2f3f5; color: #86909c; }
	.shot-pill--active .shot-pill__dot { background: #ff7d00; color: #fff; }
	.cover-template-card { display: flex; align-items: center; gap: 22rpx; margin-top: 22rpx; padding: 20rpx; border: 1rpx solid #d9e5ff; border-radius: 16rpx; background: #f7faff; }
	.cover-template-image { width: 108rpx; height: 192rpx; flex: 0 0 auto; border-radius: 10rpx; background: #eef1f5; }
	.cover-template-image--landscape { width: 192rpx; height: 108rpx; }
	.cover-template-copy { min-width: 0; flex: 1; }
	.cover-template-title { display: block; color: #1d2129; font-size: 27rpx; font-weight: 650; }
	.cover-template-desc { display: block; margin-top: 10rpx; color: #4e5969; font-size: 23rpx; line-height: 1.55; }
	.cover-template-tip { display: block; margin-top: 12rpx; color: #165dff; font-size: 21rpx; }
	.bgm-reference-card { margin-top: 18rpx; padding: 20rpx; border: 1rpx solid #e5e6eb; border-radius: 16rpx; background: #fff; }
	.bgm-reference-heading { display: block; color: #1d2129; font-size: 26rpx; font-weight: 650; }
	.bgm-reference-item { padding-top: 16rpx; margin-top: 16rpx; border-top: 1rpx solid #f2f3f5; }
	.bgm-reference-copy { margin-bottom: 14rpx; }
	.bgm-reference-title { display: block; color: #1d2129; font-size: 26rpx; font-weight: 650; }
	.bgm-reference-desc { display: block; margin-top: 6rpx; color: #86909c; font-size: 22rpx; }
	.bgm-play-button { display: flex; align-items: center; justify-content: center; width: 100%; height: 72rpx; margin: 14rpx 0 0; border: 1rpx solid #bed4ff; border-radius: 12rpx; background: #f2f7ff; color: #165dff; font-size: 24rpx; font-weight: 600; line-height: 72rpx; }
	.bgm-play-icon { margin-right: 12rpx; font-size: 22rpx; }
	.voice-reference-card { display: flex; align-items: center; justify-content: space-between; margin-top: 18rpx; padding: 20rpx; border: 1rpx solid #e5e6eb; border-radius: 16rpx; background: #fff; }
	.voice-reference-label { color: #86909c; font-size: 23rpx; }
	.voice-reference-value { color: #1d2129; font-size: 26rpx; font-weight: 650; }
	.shot-workspace { margin-top: 18rpx; }
	.sample-section { margin-top: 4rpx; }
	.sample-title { display: block; color: #1d2129; font-size: 28rpx; font-weight: 650; }
	.sample-video-card { position: relative; width: 100%; margin-top: 18rpx; overflow: hidden; border: 1rpx solid #e5e6eb; border-radius: 16rpx; background: #fff; box-shadow: 0 8rpx 22rpx rgba(29,33,41,.08); }
	.sample-video-card--ready { background: #111827; }
	.sample-video-card--portrait, .sample-video-card--landscape { width: 100%; }
	.media-card__viewport { position: relative; width: 100%; height: 360rpx; overflow: hidden; background: #111827; }
	.sample-cover { width: 100%; height: 100%; }
	.sample-cover--empty { display: flex; align-items: center; justify-content: center; background: linear-gradient(145deg, #1f2937, #020617); }
	.sample-cover-title { color: rgba(255,255,255,.72); font-size: 24rpx; font-weight: 650; }
	.sample-video__mask { position: absolute; top: 0; right: 0; bottom: 0; left: 0; display: flex; align-items: center; justify-content: center; background: linear-gradient(180deg, rgba(0,0,0,.02), rgba(0,0,0,.42)); }
	.sample-play { display: flex; align-items: center; justify-content: center; width: 82rpx; height: 82rpx; border-radius: 41rpx; background: rgba(255,255,255,.94); color: #111827; font-size: 36rpx; line-height: 82rpx; text-indent: 5rpx; box-shadow: 0 12rpx 30rpx rgba(0,0,0,.28); }
	.sample-aspect-badge { position: absolute; top: 14rpx; right: 14rpx; padding: 6rpx 14rpx; border-radius: 999rpx; background: rgba(0,0,0,.46); color: #fff; font-size: 20rpx; line-height: 1.2; backdrop-filter: blur(12rpx); -webkit-backdrop-filter: blur(12rpx); }
	.sample-video-card--empty { display: flex; box-sizing: border-box; align-items: center; justify-content: center; flex-direction: column; width: 236rpx; height: 280rpx; padding: 20rpx; border: 1rpx dashed #c9cdd4; background: #f7f8fa; box-shadow: none; }
	.media-card__footer { display: flex; align-items: center; justify-content: space-between; gap: 20rpx; padding: 18rpx 22rpx; background: #fff; }
	.media-card__title { min-width: 0; overflow: hidden; color: #1d2129; font-size: 24rpx; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
	.media-card__meta { flex: 0 0 auto; color: #86909c; font-size: 22rpx; }
	.sample-empty-title { color: #4e5969; font-size: 24rpx; font-weight: 650; }
	.sample-empty-copy { margin-top: 8rpx; color: #86909c; font-size: 21rpx; line-height: 1.45; text-align: center; }
	.upload-block { margin-top: 28rpx; padding-top: 4rpx; border-top: 1rpx solid #f2f3f5; }
	.upload-block__head { display: flex; align-items: baseline; gap: 12rpx; }
	.upload-block__title { color: #1d2129; font-size: 28rpx; font-weight: 650; }
	.upload-block__meta { color: #86909c; font-size: 22rpx; }
	.upload-requirements { margin-top: 16rpx; border: 1rpx solid #f2f3f5; border-radius: 8rpx; background: #fbfbfd; }
	.upload-requirement-row { display: flex; align-items: flex-start; padding: 18rpx 0; margin: 0 24rpx; border-bottom: 1rpx solid #f2f3f5; }
	.upload-requirement-row:last-child { border-bottom: 0; }
	.upload-requirement-label { flex: 0 0 132rpx; color: #86909c; font-size: 23rpx; }
	.upload-requirement-value { flex: 1; min-width: 0; color: #1d2129; font-size: 24rpx; line-height: 1.55; }
	.shot-preview { position: relative; display: flex; align-items: center; justify-content: center; min-height: 220rpx; margin-top: 18rpx; overflow: hidden; border: 1rpx dashed #c9cdd4; border-radius: 16rpx; background: #f7f8fa; }
	.shot-preview--ready { display: block; border-style: solid; border-color: #e5e6eb; background: #111827; box-shadow: 0 8rpx 22rpx rgba(29,33,41,.08); }
	.shot-preview__empty { text-align: center; }
	.upload-icon { display: inline-flex; align-items: center; justify-content: center; width: 62rpx; height: 62rpx; border-radius: 31rpx; background: #e8f3ff; color: #165dff; font-size: 38rpx; line-height: 62rpx; }
	.upload-title { display: block; margin-top: 14rpx; color: #1d2129; font-size: 26rpx; font-weight: 650; }
	.upload-copy { display: block; margin-top: 8rpx; color: #86909c; font-size: 22rpx; }
	.uploaded-video-cover { position: relative; width: 100%; height: 360rpx; overflow: hidden; background: #111827; }
	.uploaded-video-poster { display: block; width: 100%; height: 100%; background: #111827; }
	.uploaded-video-poster--empty { display: flex; align-items: center; justify-content: center; color: rgba(255,255,255,.58); font-size: 24rpx; }
	.uploaded-video-mask { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.16); pointer-events: none; }
	.uploaded-video-play { display: flex; align-items: center; justify-content: center; width: 86rpx; height: 86rpx; border-radius: 50%; background: rgba(255,255,255,.94); color: #1d2129; font-size: 34rpx; line-height: 86rpx; text-align: center; }
	.uploaded-video-delete { position: absolute; z-index: 4; top: 16rpx; right: 16rpx; box-sizing: border-box; width: auto; min-width: 92rpx; height: 52rpx; margin: 0; padding: 0 20rpx; border: 0; border-radius: 26rpx; background: rgba(0, 0, 0, 0.68); color: #fff; font-size: 23rpx; line-height: 52rpx; text-align: center; }
	.uploaded-video-delete::after { display: none; }
	.uploaded-video-meta { text-align: left; }
	.shot-card { margin-top: 20rpx; padding: 24rpx; border: 1rpx solid #e5e6eb; border-radius: 16rpx; background: #fff; }
	.shot-title { display: block; color: #1d2129; font-size: 28rpx; font-weight: 650; }
	.shot-requirement { display: block; max-width: 468rpx; margin-top: 8rpx; color: #4e5969; font-size: 24rpx; line-height: 1.55; }
	.shot-status { flex: 0 0 auto; margin-left: 18rpx; padding: 6rpx 12rpx; border-radius: 8rpx; background: #f2f3f5; color: #86909c; font-size: 20rpx; }
	.shot-status--done { background: #e8ffea; color: #00b42a; }
	.video-file { margin-top: 20rpx; padding: 18rpx; border-radius: 12rpx; background: #f5f6f7; }
	.video-name { display: block; color: #1d2129; font-size: 24rpx; font-weight: 600; }
	.video-meta { display: block; margin-top: 6rpx; color: #86909c; font-size: 22rpx; }
	.shot-actions { display: flex; gap: 16rpx; margin-top: 20rpx; }
	.primary-button { width: 100%; height: 88rpx; margin-top: 28rpx; overflow: hidden; border: 0; border-radius: 12rpx; background: #165dff; color: #fff; font-size: 28rpx; font-weight: 600; line-height: 88rpx; text-overflow: ellipsis; }
	.primary-button--small { flex: 1; height: 72rpx; margin-top: 0; font-size: 24rpx; line-height: 72rpx; }
	.ghost-button { min-width: 116rpx; height: 72rpx; margin: 0; padding: 0 22rpx; overflow: hidden; border: 1rpx solid #e5e6eb; border-radius: 12rpx; background: #fff; color: #4e5969; font-size: 24rpx; line-height: 72rpx; text-overflow: ellipsis; }
	.ghost-button--mini { width: auto; min-width: 132rpx; height: 60rpx; font-size: 22rpx; line-height: 60rpx; }
	.ghost-button--compact { flex: 0 0 auto; min-width: 112rpx; height: 60rpx; padding: 0 18rpx; font-size: 22rpx; line-height: 60rpx; }
	.ghost-button--danger { color: #f53f3f; }
	.compose-summary { display: flex; gap: 16rpx; margin-top: 24rpx; }
	.compose-status-card { margin-top: 20rpx; padding: 22rpx; border-radius: 16rpx; background: #fff7e8; }
	.compose-status-card--success { background: #e8ffea; }
	.compose-status-title { display: block; color: #1d2129; font-size: 25rpx; font-weight: 650; }
	.compose-status-copy { display: block; margin-top: 8rpx; color: #4e5969; font-size: 22rpx; line-height: 1.55; }
	.compose-stat { flex: 1; padding: 22rpx 12rpx; border-radius: 16rpx; background: #f5f6f7; text-align: center; }
	.compose-number { display: block; color: #1d2129; font-size: 32rpx; font-weight: 700; }
	.compose-label { display: block; margin-top: 8rpx; color: #86909c; font-size: 22rpx; }
	.compose-section { margin-top: 22rpx; padding: 24rpx; border: 1rpx solid #e5e6eb; border-radius: 16rpx; background: #fff; }
	.composed-video-card { margin-top: 22rpx; padding: 24rpx; border: 1rpx solid #e5e6eb; border-radius: 16rpx; background: #fff; }
	.composed-video-player { display: block; width: 100%; height: 360rpx; margin-top: 18rpx; overflow: hidden; border-radius: 14rpx; background: #111827; }
	.composed-video-empty { display: flex; box-sizing: border-box; flex-direction: column; align-items: center; justify-content: center; width: 100%; height: 260rpx; margin-top: 18rpx; border-radius: 14rpx; background: #f7f8fa; }
	.composed-video-empty__title { color: #4e5969; font-size: 25rpx; font-weight: 600; }
	.composed-video-empty__copy { margin-top: 10rpx; color: #86909c; font-size: 21rpx; }
	.upload-test-video-button { display: flex; align-items: center; justify-content: center; width: 100%; height: 76rpx; margin-top: 18rpx; border: 1rpx solid #c9cdd4; border-radius: 12rpx; background: #fff; color: #1d2129; font-size: 25rpx; font-weight: 600; line-height: normal; }
	.upload-test-video-button::after { display: none; }
	.save-video-button { display: flex; align-items: center; justify-content: center; width: 100%; height: 76rpx; margin-top: 18rpx; border: 1rpx solid #165dff; border-radius: 12rpx; background: #fff; color: #165dff; font-size: 25rpx; font-weight: 650; line-height: normal; }
	.save-video-button::after { display: none; }
	.save-video-button[disabled] { border-color: #c9cdd4; background: #f7f8fa; color: #86909c; opacity: 1; }
	.save-video-tip { display: block; margin-top: 10rpx; color: #86909c; font-size: 21rpx; line-height: 1.5; text-align: center; }
	.share-douyin-card { margin-top: 24rpx; padding: 24rpx; border-radius: 16rpx; background: #161823; }
	.share-douyin-title { display: block; color: #ffffff; font-size: 26rpx; font-weight: 650; }
	.share-douyin-desc { display: block; margin-top: 8rpx; color: rgba(255,255,255,.72); font-size: 22rpx; line-height: 1.55; }
	.share-douyin-button { display: flex; align-items: center; justify-content: center; width: 100%; height: 76rpx; margin-top: 18rpx; border: 0; border-radius: 12rpx; background: #fe2c55; color: #ffffff; font-size: 25rpx; font-weight: 650; line-height: normal; }
	.share-douyin-button::after { display: none; }
	.share-douyin-button[disabled] { background: #4e5969; color: rgba(255,255,255,.72); opacity: 1; }
	.compose-section-title { display: block; font-size: 27rpx; font-weight: 650; }
	.compose-section-desc { display: block; margin-top: 8rpx; color: #86909c; font-size: 21rpx; line-height: 1.5; }
	.compose-node-list { margin-top: 16rpx; }
	.compose-node { display: flex; align-items: flex-start; padding: 16rpx 0; border-bottom: 1rpx solid #f2f3f5; }
	.compose-node:last-child { border-bottom: 0; }
	.compose-node-index { display: flex; align-items: center; justify-content: center; width: 38rpx; height: 38rpx; border-radius: 19rpx; background: #e8f3ff; color: #165dff; font-size: 20rpx; font-weight: 700; }
	.compose-node-copy { flex: 1; min-width: 0; margin-left: 14rpx; }
	.compose-node-title { display: block; font-size: 24rpx; font-weight: 600; }
	.compose-node-desc { display: -webkit-box; margin-top: 6rpx; overflow: hidden; color: #86909c; font-size: 21rpx; line-height: 1.45; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
	.compose-config-row { display: flex; align-items: center; justify-content: space-between; min-height: 72rpx; border-bottom: 1rpx solid #f2f3f5; font-size: 23rpx; }
	.compose-config-row:last-child { border-bottom: 0; }
	.compose-config-value { max-width: 62%; color: #4e5969; text-align: right; }
	.state-choice-button { display: inline-flex; box-sizing: border-box; align-items: center; justify-content: center; gap: 8rpx; width: auto; min-width: 112rpx; height: 56rpx; margin: 0; padding: 0 18rpx; border: 1rpx solid #c9cdd4; border-radius: 10rpx; background: #fff; color: #4e5969; font-size: 22rpx; font-weight: 500; line-height: normal; }
	.state-choice-button::after { display: none; }
	.state-choice-button--active { border-color: #165dff; background: #edf3ff; color: #165dff; font-weight: 650; }
	.state-choice-icon { display: inline-flex; align-items: center; justify-content: center; width: 26rpx; height: 26rpx; border: 1rpx solid currentColor; border-radius: 5rpx; font-size: 18rpx; line-height: 26rpx; }
	.subtitle-setting { padding: 18rpx 0; border-bottom: 1rpx solid #f2f3f5; }
	.subtitle-setting__label { display: block; color: #1d2129; font-size: 23rpx; }
	.subtitle-mode-tabs { display: flex; margin-top: 16rpx; padding: 6rpx; border-radius: 14rpx; background: #f2f3f5; }
	.subtitle-mode-button { flex: 1; height: 60rpx; margin: 0; padding: 0; border: 0; border-radius: 10rpx; background: transparent; color: #4e5969; font-size: 23rpx; line-height: 60rpx; }
	.subtitle-mode-button::after { display: none; }
	.subtitle-mode-button--active { background: #fff; color: #165dff; font-weight: 650; box-shadow: 0 3rpx 10rpx rgba(29,33,41,.08); }
	.fixed-subtitle-list { margin: 18rpx 0; padding: 20rpx; border-radius: 14rpx; background: #f7f8fa; }
	.fixed-subtitle-tip { display: block; color: #86909c; font-size: 21rpx; line-height: 1.5; }
	.fixed-subtitle-item { margin-top: 18rpx; padding: 20rpx; border: 1rpx solid #e5e6eb; border-radius: 12rpx; background: #fff; }
	.fixed-subtitle-head { display: flex; align-items: center; justify-content: space-between; }
	.fixed-subtitle-title { color: #1d2129; font-size: 24rpx; font-weight: 650; }
	.fixed-subtitle-input { box-sizing: border-box; width: 100%; min-height: 82rpx; margin-top: 14rpx; padding: 16rpx; border-radius: 10rpx; background: #f7f8fa; color: #1d2129; font-size: 23rpx; line-height: 1.55; }
	.fixed-subtitle-input[disabled] { color: #a9aeb8; opacity: .72; }
	.check-list { margin-top: 22rpx; }
	.check-row { display: flex; align-items: flex-start; padding: 18rpx 0; border-bottom: 1rpx solid #f2f3f5; }
	.check-row:last-child { border-bottom: 0; }
	.check-mark { display: flex; align-items: center; justify-content: center; width: 40rpx; height: 40rpx; margin-right: 16rpx; border-radius: 20rpx; background: #fff7e8; color: #ff7d00; font-size: 20rpx; font-weight: 700; }
	.check-mark--ok { background: #e8ffea; color: #00b42a; }
	.check-content { flex: 1; min-width: 0; }
	.check-title { display: block; color: #1d2129; font-size: 26rpx; font-weight: 650; }
	.check-copy { display: block; margin-top: 6rpx; color: #86909c; font-size: 22rpx; line-height: 1.5; }
	.publish-box { margin-top: 24rpx; padding: 24rpx; border-radius: 16rpx; background: #f5f6f7; }
	.publish-label { display: block; color: #86909c; font-size: 22rpx; }
	.publish-topic-input { box-sizing: border-box; width: 100%; min-height: 96rpx; margin-top: 10rpx; padding: 0; border: 0; background: transparent; color: #1d2129; font-size: 28rpx; font-weight: 650; line-height: 1.5; }
	.topic-actions { display: flex; gap: 12rpx; margin-top: 18rpx; }
	.topic-input { box-sizing: border-box; flex: 1; height: 64rpx; padding: 0 18rpx; border: 1rpx solid #e5e6eb; border-radius: 12rpx; background: #fff; color: #1d2129; font-size: 24rpx; }
	.topic-add-button { flex: 0 0 112rpx; height: 64rpx; margin: 0; padding: 0; border: 1rpx solid #ff7d00; border-radius: 12rpx; background: #fff7e8; color: #ff7d00; font-size: 24rpx; font-weight: 650; line-height: 64rpx; }
	.platform-grid { display: flex; flex-wrap: wrap; gap: 16rpx; margin-top: 22rpx; }
	.platform-chip { width: calc(50% - 8rpx); height: 72rpx; margin: 0; border: 1rpx solid #e5e6eb; border-radius: 12rpx; background: #fff; color: #1d2129; font-size: 24rpx; line-height: 72rpx; }
	.platform-chip--active { border-color: #ff7d00; background: #fff7e8; color: #ff7d00; font-weight: 650; }
	.link-list { margin-top: 20rpx; }
	.link-card { margin-top: 20rpx; padding: 24rpx; border: 1rpx solid #e5e6eb; border-radius: 16rpx; background: #fff; }
	.picker-row { height: 56rpx; }
	.picker-label { color: #86909c; font-size: 22rpx; }
	.picker-value { color: #165dff; font-size: 24rpx; font-weight: 650; }
	.link-input { box-sizing: border-box; width: 100%; height: 80rpx; margin-top: 16rpx; padding: 0 20rpx; border: 1rpx solid #e5e6eb; border-radius: 12rpx; background: #f5f6f7; color: #1d2129; font-size: 24rpx; }
	.link-remove { width: 100%; margin-top: 16rpx; }
	.success-copy { display: block; margin-top: 18rpx; color: #00b42a; font-size: 24rpx; }
	.state-card { text-align: center; }
	.state-card--error { margin-top: 32rpx; padding-top: 70rpx; padding-bottom: 70rpx; }
	.state-button { width: auto; padding: 0 34rpx; }
	.bottom-action { position: fixed; right: 32rpx; bottom: calc(112rpx + env(safe-area-inset-bottom)); left: 32rpx; z-index: 10; padding: 18rpx 0; background: rgba(245,246,247,.92); }
	.bottom-action__button { margin-top: 0; }
</style>
