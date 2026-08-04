<template>
	<view class="task-page">
		<view class="page-top">
			<button class="back-button" aria-label="返回" @click="goBack">‹</button>
			<view class="heading-copy">
				<text class="page-title">AI短视频任务</text>
				<text class="page-subtitle">后管发布计划下发后，在这里完成拍摄、发布和回传</text>
			</view>
		</view>

		<view class="summary-card">
			<view class="summary-main">
				<text class="summary-number">{{ taskCount }}</text>
				<text class="summary-label">{{ currentFilterLabel }}</text>
			</view>
			<text class="summary-copy">任务数据来自后端员工内容任务接口，状态与执行步骤保持一致</text>
		</view>

		<view class="task-sticky-tabs">
			<scroll-view class="filter-scroll" scroll-x :show-scrollbar="false">
				<view class="filter-row">
					<button
						v-for="item in filters"
						:key="item.value"
						class="filter-pill"
						:class="{ 'filter-pill--active': category === item.value }"
						@click="changeCategory(item.value)"
					>{{ item.label }}</button>
				</view>
			</scroll-view>

			<view class="search-box">
				<image class="search-icon" src="/static/icons/nav/search.svg" mode="aspectFit" />
				<input v-model.trim="keyword" class="search-input" placeholder="搜索计划、活动或任务文案" confirm-type="search" />
				<button v-if="keyword" class="clear-button" aria-label="清空搜索" @click="keyword = ''">×</button>
			</view>
		</view>

		<view v-if="loading" class="state-card">
			<view class="loading-ring" />
			<text class="state-title">正在加载任务</text>
			<text class="state-copy">正在同步后管下发的最新计划</text>
		</view>

		<view v-else-if="errorMessage" class="state-card">
			<text class="state-icon">!</text>
			<text class="state-title">任务加载失败</text>
			<text class="state-copy">{{ errorMessage }}</text>
			<button class="state-button" @click="load">重新加载</button>
		</view>

		<view v-else-if="!visibleTasks.length" class="state-card">
			<text class="empty-mark">✓</text>
			<text class="state-title">{{ emptyTitle }}</text>
			<text class="state-copy">后管下发新的短视频发布计划后，会自动出现在这里</text>
		</view>

		<view v-else class="task-list">
			<button
				v-for="task in visibleTasks"
				:key="task.id"
				class="task-card"
				hover-class="task-card--pressed"
				@click="openTask(task.id)"
			>
				<view class="task-card__top">
					<text class="activity-name">{{ task.activityName || 'AI短视频' }}</text>
					<text class="stage-tag" :class="`stage-tag--${tone(task.stage)}`">{{ task.stageLabel }}</text>
				</view>
				<view class="task-name-line"><text class="task-name">{{ task.planName }}</text><text class="creation-tag">{{ creationLabel(task) }}</text></view>
				<text class="task-instruction">{{ task.taskInstruction }}</text>
				<view class="task-meta">
					<text class="deadline" :class="{ 'deadline--danger': isOverdue(task.deadline) }">{{ deadline(task.deadline) }}</text>
					<text class="task-arrow">›</text>
				</view>
				<view class="next-step">
					<text class="next-step__label">下一步</text>
					<text class="next-step__value">{{ task.actionHint }}</text>
				</view>
			</button>
		</view>
		<app-tab-bar :active-index="2" @change="changeTab" />
	</view>
</template>

<script>
	import { fetchContentTasks } from '@/api/content-tasks.js'
	import { categoryLabel, contentCreationLabel, formatDeadline, stageTone } from '@/utils/content-task.js'
	import AppTabBar from '@/components/app-tab-bar/app-tab-bar.vue'

	export default {
		components: { AppTabBar },
		data() {
			return {
				category: 'TODO',
				keyword: '',
				loading: true,
				errorMessage: '',
				tasks: [],
				filters: [
					{ label: '全部', value: 'ALL' },
					{ label: '待完成', value: 'TODO' },
					{ label: '处理中', value: 'PROCESSING' },
					{ label: '已完成', value: 'COMPLETED' },
					{ label: '异常', value: 'EXCEPTION' }
				]
			}
		},
		computed: {
			visibleTasks() {
				if (!this.keyword) return this.tasks
				const keyword = this.keyword.toLowerCase()
				return this.tasks.filter(task =>
					String(task.planName || '').toLowerCase().includes(keyword) ||
					String(task.activityName || '').toLowerCase().includes(keyword) ||
					String(task.taskInstruction || '').toLowerCase().includes(keyword)
				)
			},
			taskCount() { return this.visibleTasks.length },
			currentFilterLabel() { return categoryLabel(this.category) },
			emptyTitle() {
				if (this.keyword) return '没有找到匹配的任务'
				return this.category === 'TODO' ? '当前没有待完成任务' : '该分类下暂无任务'
			}
		},
		onLoad(options = {}) {
			if (this.filters.some(item => item.value === options.category)) this.category = options.category
			this.load()
		},
		async onPullDownRefresh() {
			try { await this.load(false) } finally { uni.stopPullDownRefresh() }
		},
		methods: {
			async load(showLoading = true) {
				if (showLoading) this.loading = true
				this.errorMessage = ''
				try {
					this.tasks = await fetchContentTasks({ category: this.category, pageSize: 50 })
				} catch (error) {
					this.errorMessage = error instanceof Error ? error.message : '请检查网络后重试'
				} finally {
					this.loading = false
				}
			},
			changeCategory(value) {
				if (value === this.category) return
				this.category = value
				this.keyword = ''
				this.load()
			},
			openTask(id) { uni.navigateTo({ url: `/pages/content-tasks/detail?id=${id}` }) },
			goBack() { uni.navigateBack({ fail: () => uni.reLaunch({ url: '/pages/home/index' }) }) },
			changeTab(index) {
				if (index === 2) return
				uni.reLaunch({ url: `/pages/home/index?tab=${index}` })
			},
			tone: stageTone,
			deadline: formatDeadline,
			isOverdue(value) { return value && new Date(value).getTime() < Date.now() },
			creationLabel: contentCreationLabel
		}
	}
</script>

<style scoped>
	.task-page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: calc(env(safe-area-inset-top) + 140rpx) 32rpx calc(150rpx + env(safe-area-inset-bottom));
		background: #f5f6f7;
		color: #1d2129;
		font-family: "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
	}
	button::after { border: 0; }
	.page-top { position: fixed; top: 0; right: 0; left: 0; z-index: 60; box-sizing: border-box; display: flex; align-items: center; min-height: calc(env(safe-area-inset-top) + 112rpx); padding: calc(env(safe-area-inset-top) + 18rpx) 32rpx 18rpx; background: rgba(245,246,247,.98); backdrop-filter: blur(18rpx); -webkit-backdrop-filter: blur(18rpx); }
	.back-button { width: 64rpx; height: 64rpx; margin: 0 16rpx 0 0; padding: 0; border: 0; border-radius: 12rpx; background: #fff; color: #1d2129; font-size: 48rpx; line-height: 58rpx; }
	.heading-copy { flex: 1; min-width: 0; }
	.page-title { display: block; font-size: 40rpx; font-weight: 650; line-height: 1.35; }
	.page-subtitle { display: block; margin-top: 8rpx; color: #4e5969; font-size: 24rpx; line-height: 1.45; }
	.summary-card { margin-top: 32rpx; padding: 32rpx; border: 1rpx solid #e5e6eb; border-radius: 16rpx; background: #fff; }
	.summary-main { display: flex; align-items: flex-end; }
	.summary-number { color: #165dff; font-size: 56rpx; font-weight: 700; line-height: 1; }
	.summary-label { margin-left: 12rpx; color: #1d2129; font-size: 28rpx; font-weight: 600; line-height: 1.15; }
	.summary-copy { display: block; margin-top: 16rpx; color: #86909c; font-size: 24rpx; line-height: 1.5; }
	.task-sticky-tabs { position: sticky; top: calc(env(safe-area-inset-top) + 112rpx); z-index: 50; margin: 24rpx -32rpx 0; padding: 16rpx 32rpx 18rpx; background: rgba(245,246,247,.96); backdrop-filter: blur(18rpx); -webkit-backdrop-filter: blur(18rpx); }
	.filter-scroll { width: 100%; white-space: nowrap; }
	.filter-row { display: inline-flex; padding-right: 32rpx; }
	.filter-pill { height: 64rpx; margin: 0 12rpx 0 0; padding: 0 24rpx; border: 1rpx solid #e5e6eb; border-radius: 12rpx; background: #fff; color: #4e5969; font-size: 24rpx; line-height: 64rpx; }
	.filter-pill--active { border-color: #165dff; background: #165dff; color: #fff; font-weight: 600; }
	.search-box { display: flex; align-items: center; height: 80rpx; margin-top: 16rpx; padding: 0 24rpx; border: 1rpx solid #e5e6eb; border-radius: 12rpx; background: #fff; }
	.search-icon { flex: 0 0 auto; width: 32rpx; height: 32rpx; opacity: .58; }
	.search-input { flex: 1; min-width: 0; height: 80rpx; margin-left: 16rpx; color: #1d2129; font-size: 26rpx; }
	.clear-button { width: 48rpx; height: 48rpx; margin: 0; padding: 0; border: 0; border-radius: 24rpx; background: #f2f3f5; color: #86909c; font-size: 30rpx; line-height: 45rpx; }
	.task-list { margin-top: 24rpx; }
	.task-card { box-sizing: border-box; width: 100%; margin: 0 0 24rpx; padding: 32rpx; border: 1rpx solid #e5e6eb; border-radius: 16rpx; background: #fff; text-align: left; }
	.task-card--pressed { background: #f2f3f5; transform: scale(.99); }
	.task-card__top, .task-meta { display: flex; align-items: center; justify-content: space-between; }
	.activity-name { overflow: hidden; color: #86909c; font-size: 22rpx; text-overflow: ellipsis; white-space: nowrap; }
	.stage-tag { flex: 0 0 auto; margin-left: 16rpx; padding: 8rpx 12rpx; border-radius: 8rpx; font-size: 20rpx; line-height: 1; }
	.stage-tag--primary { background: #e8f3ff; color: #165dff; }
	.stage-tag--warning { background: #fff7e8; color: #ff7d00; }
	.stage-tag--success { background: #e8ffea; color: #00b42a; }
	.stage-tag--danger { background: #fff1f0; color: #f53f3f; }
	.stage-tag--muted { background: #f2f3f5; color: #4e5969; }
	.task-name { display: block; margin-top: 20rpx; font-size: 32rpx; font-weight: 650; line-height: 1.4; }
	.task-name-line { display: flex; align-items: center; min-width: 0; margin-top: 20rpx; }
	.task-name-line .task-name { min-width: 0; margin-top: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
	.creation-tag { flex: 0 0 auto; margin-left: 12rpx; padding: 6rpx 10rpx; border: 1rpx solid #bed4ff; border-radius: 8rpx; background: #f2f7ff; color: #165dff; font-size: 19rpx; line-height: 1; }
	.task-instruction { display: -webkit-box; margin-top: 12rpx; overflow: hidden; color: #4e5969; font-size: 24rpx; line-height: 1.6; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
	.task-meta { margin-top: 20rpx; color: #86909c; font-size: 22rpx; }
	.deadline--danger { color: #f53f3f; }
	.task-arrow { color: #c9cdd4; font-size: 34rpx; }
	.next-step { display: flex; margin-top: 20rpx; padding: 20rpx; border-radius: 12rpx; background: #f7f8fa; font-size: 24rpx; line-height: 1.5; }
	.next-step__label { flex: 0 0 auto; color: #165dff; font-weight: 600; }
	.next-step__value { margin-left: 16rpx; color: #4e5969; }
	.state-card { display: flex; flex-direction: column; align-items: center; margin-top: 32rpx; padding: 80rpx 32rpx; border: 1rpx solid #e5e6eb; border-radius: 16rpx; background: #fff; text-align: center; }
	.state-icon, .empty-mark { display: flex; align-items: center; justify-content: center; width: 72rpx; height: 72rpx; border-radius: 36rpx; background: #fff1f0; color: #f53f3f; font-size: 34rpx; font-weight: 700; }
	.empty-mark { background: #e8ffea; color: #00b42a; }
	.state-title { margin-top: 24rpx; font-size: 28rpx; font-weight: 600; }
	.state-copy { margin-top: 12rpx; color: #86909c; font-size: 24rpx; line-height: 1.55; }
	.state-button { height: 72rpx; margin-top: 28rpx; padding: 0 32rpx; border: 0; border-radius: 12rpx; background: #165dff; color: #fff; font-size: 24rpx; line-height: 72rpx; }
	.loading-ring { width: 48rpx; height: 48rpx; border: 6rpx solid #e8f3ff; border-top-color: #165dff; border-radius: 24rpx; animation: spin .8s linear infinite; }
	@keyframes spin { to { transform: rotate(360deg); } }
</style>
