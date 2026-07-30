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
					<text class="stage-tag" :class="`stage-tag--${tone(task.stage)}`">{{ task.stageLabel || '待处理' }}</text>
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
						@click="activeStepIndex = index"
					>
						<text class="step-tab__index">{{ index + 1 }}</text>
						<text class="step-tab__title">{{ step.shortTitle }}</text>
					</button>
				</view>
			</scroll-view>

			<view v-if="activeStepIndex === 0" class="panel-card">
				<view class="section-head">
					<view>
						<text class="card-title">确认后管下发内容</text>
					</view>
					<button class="ghost-button ghost-button--mini" @click="copyText(task.taskInstruction, '任务文案')">复制文案</button>
				</view>
				<view class="instruction-box" :class="{ 'instruction-box--collapsed': !showFullInstruction }">
					<text class="instruction">{{ task.taskInstruction || '后管暂未填写任务文案，请联系负责人补充。' }}</text>
				</view>
				<button v-if="hasLongInstruction" class="text-button" @click="showFullInstruction = !showFullInstruction">
					{{ showFullInstruction ? '收起文案' : '展开完整文案' }}
				</button>
				<view class="detail-grid">
					<view class="detail-item"><text class="detail-label">所属活动</text><text class="detail-value">{{ task.activityName || '—' }}</text></view>
					<view class="detail-item"><text class="detail-label">创建时间</text><text class="detail-value">{{ createdText }}</text></view>
					<view class="detail-item"><text class="detail-label">当前状态</text><text class="detail-value">{{ task.stageLabel || '—' }}</text></view>
					<view class="detail-item"><text class="detail-label">任务截止</text><text class="detail-value">{{ deadlineText }}</text></view>
				</view>
				<view class="notice-box">
					<text class="notice-title">执行提示</text>
					<text class="notice-copy">{{ actionDescription }}</text>
				</view>
			</view>

			<view v-if="activeStepIndex === 1" class="panel-card">
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
				<view class="shot-workspace">
					<view v-if="showSampleSection" class="sample-section">
						<text class="sample-title">样例：</text>
						<view v-if="activeShot.sampleVideoUrl" class="sample-video-card">
							<video
								class="sample-video"
								:src="activeShot.sampleVideoUrl"
								:poster="activeShot.sampleCoverUrl"
								:controls="true"
								:show-center-play-btn="true"
								object-fit="cover"
							/>
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
					<view class="shot-preview" @click="chooseShotVideo(activeShotIndex)">
						<view v-if="activeShotVideo" class="shot-preview__file">
							<text class="video-name">{{ activeShotVideo.name }}</text>
							<text class="video-meta">{{ formatVideoMeta(activeShotVideo) }}</text>
						</view>
						<view v-else class="shot-preview__empty">
							<text class="upload-icon">＋</text>
							<text class="upload-title">{{ isOriginalTask ? '上传视频' : `上传 ${activeShot.title} 视频` }}</text>
							<text class="upload-copy">支持拍摄或从相册选择，建议 9:16 竖屏</text>
						</view>
					</view>
					<view class="shot-toolbar">
						<button v-if="!isOriginalTask" class="ghost-button ghost-button--compact" :disabled="activeShotIndex === 0" @click="switchShot(-1)">上一个</button>
						<button v-if="activeShotVideo" class="ghost-button ghost-button--compact" @click="previewShotVideo(activeShotIndex)">预览</button>
						<button class="ghost-button ghost-button--compact" @click="chooseShotVideo(activeShotIndex)">{{ activeShotVideo ? '修改视频' : '选择视频' }}</button>
						<button v-if="activeShotVideo" class="ghost-button ghost-button--compact ghost-button--danger" @click="removeShotVideo(activeShotIndex)">删除</button>
						<button v-if="!isOriginalTask" class="ghost-button ghost-button--compact" :disabled="activeShotIndex === shotSlots.length - 1" @click="switchShot(1)">下一个</button>
					</view>
				</view>
			</view>

			<view v-if="activeStepIndex === 2" class="panel-card">
				<view class="section-head">
					<view>
						<text class="card-title">生成成片前检查素材</text>
					</view>
					<text class="section-count">{{ composeStateText }}</text>
				</view>
				<view class="compose-summary">
					<view class="compose-stat">
						<text class="compose-number">{{ uploadedCount }}</text>
						<text class="compose-label">已上传分镜</text>
					</view>
					<view class="compose-stat">
						<text class="compose-number">{{ missingShotCount }}</text>
						<text class="compose-label">缺少分镜</text>
					</view>
					<view class="compose-stat">
						<text class="compose-number">{{ draft.composeReady ? '是' : '否' }}</text>
						<text class="compose-label">合成草稿</text>
					</view>
				</view>
				<view class="check-list">
					<view v-for="item in composeChecks" :key="item.title" class="check-row">
						<text class="check-mark" :class="{ 'check-mark--ok': item.ok }">{{ item.ok ? '✓' : '!' }}</text>
						<view class="check-content">
							<text class="check-title">{{ item.title }}</text>
							<text class="check-copy">{{ item.copy }}</text>
						</view>
					</view>
				</view>
				<text class="helper-text">当前移动端先保存合成草稿；后端开放合成提交接口后，这里可直接切换为真实提交。</text>
			</view>

			<view v-if="activeStepIndex === 3" class="panel-card">
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
						:auto-height="true"
						@blur="saveDraft"
					/>
					<view class="topic-actions">
						<input
							v-model.trim="topicDraft"
							class="topic-input"
							placeholder="添加话题，如 中秋"
							@confirm="addTopic"
						/>
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
				<view class="notice-box">
					<text class="notice-title">发布动作</text>
					<text class="notice-copy">打开对应平台发布视频，标题可使用后管任务文案，发布完成后回到下一步填写视频链接。</text>
				</view>
			</view>

			<view v-if="activeStepIndex === 4" class="panel-card">
				<view class="section-head">
					<view>
						<text class="card-title">填写已发布视频链接</text>
					</view>
					<button class="ghost-button ghost-button--mini" @click="addPublishLink">新增平台</button>
				</view>
				<view class="link-list">
					<view v-for="(link, index) in draft.publishLinks" :key="`${link.platform}-${index}`" class="link-card">
						<picker :range="platformOptions" :value="platformIndex(link.platform)" @change="changeLinkPlatform(index, $event)">
							<view class="picker-row">
								<text class="picker-label">发布平台</text>
								<text class="picker-value">{{ link.platform }}</text>
							</view>
						</picker>
						<input
							v-model.trim="link.url"
							class="link-input"
							placeholder="粘贴视频链接，例如 https://..."
							@blur="saveDraft"
						/>
						<button v-if="draft.publishLinks.length > 1" class="ghost-button ghost-button--danger link-remove" @click="removePublishLink(index)">删除</button>
					</view>
				</view>
				<text v-if="draft.linksSaved" class="success-copy">已保存回传草稿：{{ draft.updatedAt }}</text>
			</view>

			<view class="bottom-action">
				<button class="primary-button bottom-action__button" @click="handlePrimaryAction">{{ primaryActionLabel }}</button>
			</view>
		</template>

		<app-tab-bar :active-index="2" @change="changeTab" />
	</view>
</template>

<script>
	import { fetchContentTask } from '@/api/content-tasks.js'
	import { appConfig } from '@/config/app.js'
	import { formatDeadline, stageTone } from '@/utils/content-task.js'
	import AppTabBar from '@/components/app-tab-bar/app-tab-bar.vue'

	const DEFAULT_DRAFT = Object.freeze({
		videos: [],
		composeReady: false,
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
				topicDraft: '',
				draft: { ...DEFAULT_DRAFT, videos: [], publishLinks: [{ platform: '抖音', url: '' }] },
				platformOptions: ['抖音', '视频号', '小红书', '快手']
			}
		},
		computed: {
			taskDraftKey() { return `content-task-draft:${this.id}` },
			deadlineText() { return this.task ? formatDeadline(this.task.deadline) : '' },
			isOverdue() { return this.task?.deadline && new Date(this.task.deadline).getTime() < Date.now() },
			createdText() { return this.task?.createdTime ? new Date(this.task.createdTime).toLocaleString('zh-CN', { hour12: false }) : '—' },
			hasLongInstruction() { return (this.task?.taskInstruction || '').length > 140 },
			workbenchSteps() {
				return [
					{ title: '任务信息', shortTitle: '任务', desc: '确认计划名称、任务文案、截止时间和状态' },
					{ title: '视频上传', shortTitle: '上传', desc: '按分镜样例或原创拍摄要求上传竖屏视频' },
					{ title: '视频合成', shortTitle: '合成', desc: '检查素材并生成合成预览' },
					{ title: '视频发布', shortTitle: '发布', desc: '复制话题，发布到指定平台' },
					{ title: '回传链接', shortTitle: '回传', desc: '填写发布链接，完成移动端闭环' }
				]
			},
			isOriginalTask() {
				const mode = String(this.task?.creationMode || '').toUpperCase()
				if (mode) return mode === 'SELF_CREATED'
				const source = this.task?.taskInstruction || ''
				return /拍摄要求[:：]/.test(source) && !/分镜要求[:：]/.test(source)
			},
			showSampleSection() {
				return !this.isOriginalTask
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
						durationText: apiItem.durationText || parsedItem.durationText || '约8s'
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
				return Array.from({ length: this.storyboardCount }, (_, index) => ({
					key: `shot-${index + 1}`,
					title: this.isOriginalTask ? '视频' : `分镜 ${index + 1}`,
					requirement: items[index]?.requirement || fallback[index % fallback.length],
					voiceover: items[index]?.voiceover || '根据后管任务文案生成/朗读当前分镜配音，保持自然语速。',
					sampleVideoUrl: items[index]?.sampleVideoUrl || '',
					sampleCoverUrl: items[index]?.sampleCoverUrl || '',
					durationText: items[index]?.durationText || '约8s'
				}))
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
				if (this.task?.stage === 'READY_TO_SHOOT') return '查看文案和拍摄要求，按分镜上传素材。'
				if (this.task?.stage === 'SHOOTING') return '继续补齐剩余分镜素材，准备合成。'
				if (this.task?.stage === 'PROCESSING') return '系统正在处理作品，可先核对已保存草稿。'
				if (this.task?.stage === 'PENDING_REVIEW') return '作品已提交审核，可保留发布素材，等待审核结果。'
				if (this.task?.stage === 'READY_TO_PUBLISH') return '复制话题发布视频，发布后填写链接回传。'
				if (this.task?.stage === 'COMPLETED') return '任务已完成，后续数据由后台汇总。'
				return '按当前步骤完成后，任务会进入下一阶段。'
			},
			composeStateText() {
				if (this.draft.composeReady) return '已生成草稿'
				if (this.uploadedCount === 0) return '待上传'
				if (this.missingShotCount > 0) return '可预合成'
				return '可合成'
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
			primaryActionLabel() {
				if (this.activeStepIndex === 0) return '进入视频上传'
				if (this.activeStepIndex === 1) return this.uploadedCount > 0 ? '检查并合成' : '上传第一个视频'
				if (this.activeStepIndex === 2) return this.draft.composeReady ? '去发布视频' : '生成合成预览'
				if (this.activeStepIndex === 3) return '填写回传链接'
				return '保存回传信息'
			}
		},
		onLoad(options) {
			this.id = options.id || ''
			this.load()
		},
		onShow() {
			if (this.id) this.loadDraft()
		},
		methods: {
			resolveAssetUrl(url) {
				if (!url) return ''
				if (/^https?:\/\//i.test(url)) return url
				return `${appConfig.apiBaseUrl.replace(/\/$/, '')}/${String(url).replace(/^\//, '')}`
			},
			parseStoryboardText(rawText) {
				const text = String(rawText || '').replace(/^\d+[.．、]\s*/, '').trim()
				const pick = (label) => {
					const match = text.match(new RegExp(`${label}[:：]\\s*(https?:\\/\\/[^\\s；;|｜]+)`))
					return match ? match[1] : ''
				}
				const scriptMatch = text.match(/(?:台词|配音文案)[:：]([^；;|｜]+)/)
				const durationMatch = text.match(/(?:时长要求|时长)[:：]?\s*([^；;|｜]+)/)
				return {
					requirement: text
						.replace(/(?:台词|配音文案)[:：][^；;|｜]+/g, '')
						.replace(/(?:样例视频|样例封面|示例视频|示例封面)[:：]\s*https?:\/\/[^\s；;|｜]+/g, '')
						.replace(/(?:时长要求|时长)[:：]?\s*[^；;|｜]+/g, '')
						.replace(/[；;|｜]+$/g, '')
						.trim(),
					voiceover: scriptMatch ? scriptMatch[1].trim() : '',
					sampleVideoUrl: pick('样例视频') || pick('示例视频'),
					sampleCoverUrl: pick('样例封面') || pick('示例封面'),
					durationText: durationMatch ? durationMatch[1].trim() : ''
				}
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
				const stageSteps = {
					LOCKED: 0,
					READY_TO_SHOOT: 1,
					SHOOTING: 1,
					NEEDS_REVISION: 1,
					PROCESSING: 2,
					PENDING_REVIEW: 2,
					READY_TO_PUBLISH: 3,
					COMPLETED: 4
				}
				return stageSteps[stage] ?? 0
			},
			loadDraft() {
				if (!this.id) return
				const cached = uni.getStorageSync(this.taskDraftKey)
				if (!cached) {
					this.draft = { ...DEFAULT_DRAFT, videos: [], publishTopic: this.defaultPublishTopic, selectedPlatforms: ['抖音'], publishLinks: [{ platform: '抖音', url: '' }] }
					return
				}
				const selectedPlatforms = Array.isArray(cached.selectedPlatforms) && cached.selectedPlatforms.length
					? cached.selectedPlatforms
					: (cached.lastPlatform ? [cached.lastPlatform] : ['抖音'])
				this.draft = {
					...DEFAULT_DRAFT,
					...cached,
					videos: Array.isArray(cached.videos) ? cached.videos.slice(0, this.storyboardCount) : [],
					publishTopic: cached.publishTopic || this.defaultPublishTopic,
					selectedPlatforms,
					publishLinks: Array.isArray(cached.publishLinks) && cached.publishLinks.length ? cached.publishLinks : [{ platform: '抖音', url: '' }]
				}
			},
			saveDraft() {
				this.draft = { ...this.draft, updatedAt: new Date().toLocaleString('zh-CN', { hour12: false }) }
				uni.setStorageSync(this.taskDraftKey, this.draft)
			},
			handlePrimaryAction() {
				if (this.activeStepIndex === 0) {
					this.activeStepIndex = 1
					return
				}
				if (this.activeStepIndex === 1) {
					if (!this.activeShotVideo) this.chooseShotVideo(this.activeShotIndex)
					else this.activeStepIndex = 2
					return
				}
				if (this.activeStepIndex === 2) {
					if (!this.draft.composeReady) this.prepareCompose()
					else this.activeStepIndex = 3
					return
				}
				if (this.activeStepIndex === 3) {
					this.activeStepIndex = 4
					return
				}
				this.savePublishLinks()
			},
			chooseShotVideo(index) {
				uni.chooseVideo({
					count: 1,
					compressed: true,
					maxDuration: 90,
					sourceType: ['album', 'camera'],
					success: (res) => {
						const videos = [...this.draft.videos]
						videos[index] = {
							name: `分镜${index + 1}视频`,
							path: res.tempFilePath,
							duration: Number(res.duration || 0),
							size: Number(res.size || 0)
						}
						this.draft = { ...this.draft, videos, composeReady: false }
						this.saveDraft()
						uni.showToast({ title: '视频已保存到草稿', icon: 'success' })
						if (index < this.shotSlots.length - 1 && !videos[index + 1]) this.activeShotIndex = index + 1
					}
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
				if (uni.previewMedia) {
					uni.previewMedia({ sources: [{ url: video.path, type: 'video' }] })
					return
				}
				uni.showToast({ title: '当前端暂不支持预览', icon: 'none' })
			},
			previewSampleVideo(shot) {
				if (!shot?.sampleVideoUrl) {
					uni.showToast({ title: '暂无样例视频', icon: 'none' })
					return
				}
				if (uni.previewMedia) {
					uni.previewMedia({ sources: [{ url: shot.sampleVideoUrl, type: 'video', poster: shot.sampleCoverUrl }] })
					return
				}
				uni.setClipboardData({
					data: shot.sampleVideoUrl,
					success: () => uni.showToast({ title: '已复制样例视频链接', icon: 'none' })
				})
			},
			removeShotVideo(index) {
				const videos = [...this.draft.videos]
				videos[index] = null
				this.draft = { ...this.draft, videos, composeReady: false }
				this.saveDraft()
			},
			prepareCompose() {
				if (this.uploadedCount === 0) {
					uni.showToast({ title: '请先上传视频素材', icon: 'none' })
					return
				}
				this.draft = { ...this.draft, composeReady: true }
				this.saveDraft()
				uni.showToast({ title: '合成草稿已生成', icon: 'success' })
				this.activeStepIndex = 3
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
				return Array.isArray(this.draft.selectedPlatforms) && this.draft.selectedPlatforms.includes(platform)
			},
			togglePlatform(platform) {
				const selected = Array.isArray(this.draft.selectedPlatforms) ? [...this.draft.selectedPlatforms] : []
				const exists = selected.includes(platform)
				const next = exists ? selected.filter((item) => item !== platform) : [...selected, platform]
				const safeNext = next.length ? next : [platform]
				const publishLinks = safeNext.map((item) => {
					const existed = this.draft.publishLinks.find((link) => link.platform === item)
					return existed || { platform: item, url: '' }
				})
				this.draft = { ...this.draft, selectedPlatforms: safeNext, lastPlatform: platform, publishLinks }
				this.saveDraft()
			},
			addPublishLink() {
				this.draft = {
					...this.draft,
					publishLinks: [...this.draft.publishLinks, { platform: this.platformOptions[0], url: '' }]
				}
				this.saveDraft()
			},
			removePublishLink(index) {
				this.draft = {
					...this.draft,
					publishLinks: this.draft.publishLinks.filter((_, itemIndex) => itemIndex !== index)
				}
				this.saveDraft()
			},
			changeLinkPlatform(index, event) {
				const platform = this.platformOptions[Number(event.detail.value)] || this.platformOptions[0]
				const publishLinks = this.draft.publishLinks.map((item, itemIndex) => itemIndex === index ? { ...item, platform } : item)
				this.draft = { ...this.draft, publishLinks }
				this.saveDraft()
			},
			platformIndex(platform) {
				const index = this.platformOptions.indexOf(platform)
				return index >= 0 ? index : 0
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
	.instruction-box { position: relative; margin-top: 20rpx; overflow: hidden; }
	.instruction-box--collapsed { max-height: 188rpx; }
	.instruction-box--collapsed::after { position: absolute; right: 0; bottom: 0; left: 0; height: 64rpx; content: ""; background: linear-gradient(180deg, rgba(255,255,255,0), #fff); }
	.instruction { display: block; color: #4e5969; font-size: 26rpx; line-height: 1.7; }
	.text-button { width: auto; height: 54rpx; margin: 12rpx 0 0; padding: 0; border: 0; background: transparent; color: #165dff; font-size: 24rpx; line-height: 54rpx; text-align: left; }
	.detail-grid { display: flex; flex-wrap: wrap; margin-top: 24rpx; padding-top: 24rpx; border-top: 1rpx solid #f2f3f5; }
	.detail-item { box-sizing: border-box; width: 50%; padding: 0 16rpx 20rpx 0; }
	.detail-label { display: block; color: #86909c; font-size: 22rpx; }
	.detail-value { display: block; margin-top: 8rpx; color: #1d2129; font-size: 24rpx; line-height: 1.45; }
	.notice-box { margin-top: 24rpx; padding: 24rpx; border-radius: 16rpx; background: #f2f7ff; }
	.notice-title { display: block; color: #165dff; font-size: 24rpx; font-weight: 650; }
	.notice-copy, .helper-text, .state-copy { display: block; margin-top: 10rpx; color: #4e5969; font-size: 24rpx; line-height: 1.6; }
	.section-count { color: #165dff; font-size: 28rpx; font-weight: 650; }
	.shot-switcher { position: sticky; top: calc(env(safe-area-inset-top) + 186rpx); z-index: 45; width: auto; margin: 22rpx -28rpx 0; padding: 12rpx 28rpx; background: rgba(255,255,255,.96); white-space: nowrap; backdrop-filter: blur(16rpx); -webkit-backdrop-filter: blur(16rpx); }
	.shot-switcher__inner { display: inline-flex; gap: 12rpx; min-width: 100%; }
	.shot-pill { display: inline-flex; align-items: center; width: auto; height: 60rpx; margin: 0; padding: 0 18rpx; border: 1rpx solid #e5e6eb; border-radius: 30rpx; background: #fff; color: #4e5969; font-size: 23rpx; line-height: 60rpx; }
	.shot-pill--done { border-color: #e5e6eb; background: #fff; color: #4e5969; font-weight: 400; }
	.shot-pill--active { border-color: #ff7d00; background: #fff7e8; color: #ff7d00; font-weight: 650; }
	.shot-pill__dot { display: inline-flex; align-items: center; justify-content: center; width: 30rpx; height: 30rpx; margin-right: 8rpx; border-radius: 15rpx; background: #f2f3f5; color: inherit; font-size: 18rpx; font-weight: 700; line-height: 30rpx; }
	.shot-pill--done .shot-pill__dot { background: #f2f3f5; color: #86909c; }
	.shot-pill--active .shot-pill__dot { background: #ff7d00; color: #fff; }
	.shot-workspace { margin-top: 18rpx; }
	.sample-section { margin-top: 4rpx; }
	.sample-title { display: block; color: #1d2129; font-size: 28rpx; font-weight: 650; }
	.sample-video-card { position: relative; width: 236rpx; height: 280rpx; margin-top: 18rpx; overflow: hidden; border-radius: 12rpx; background: #eef1f5; box-shadow: 0 8rpx 22rpx rgba(29,33,41,.08); }
	.sample-video { width: 100%; height: 100%; }
	.sample-video__mask { position: absolute; top: 0; right: 0; bottom: 0; left: 0; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.18); }
	.sample-play { display: flex; align-items: center; justify-content: center; width: 72rpx; height: 72rpx; border-radius: 36rpx; background: rgba(255,255,255,.88); color: #1d2129; font-size: 34rpx; line-height: 72rpx; text-indent: 4rpx; }
	.sample-video-card--empty { display: flex; box-sizing: border-box; align-items: center; justify-content: center; flex-direction: column; padding: 20rpx; border: 1rpx dashed #c9cdd4; background: #f7f8fa; box-shadow: none; }
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
	.shot-preview { display: flex; align-items: center; justify-content: center; min-height: 220rpx; margin-top: 18rpx; border: 1rpx dashed #c9cdd4; border-radius: 16rpx; background: #f7f8fa; }
	.shot-preview__empty { text-align: center; }
	.upload-icon { display: inline-flex; align-items: center; justify-content: center; width: 62rpx; height: 62rpx; border-radius: 31rpx; background: #e8f3ff; color: #165dff; font-size: 38rpx; line-height: 62rpx; }
	.upload-title { display: block; margin-top: 14rpx; color: #1d2129; font-size: 26rpx; font-weight: 650; }
	.upload-copy { display: block; margin-top: 8rpx; color: #86909c; font-size: 22rpx; }
	.shot-preview__file { width: 100%; padding: 30rpx; text-align: center; }
	.shot-toolbar { display: flex; flex-wrap: nowrap; gap: 12rpx; margin-top: 20rpx; overflow-x: auto; }
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
	.compose-stat { flex: 1; padding: 22rpx 12rpx; border-radius: 16rpx; background: #f5f6f7; text-align: center; }
	.compose-number { display: block; color: #1d2129; font-size: 32rpx; font-weight: 700; }
	.compose-label { display: block; margin-top: 8rpx; color: #86909c; font-size: 22rpx; }
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
