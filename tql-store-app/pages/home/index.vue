<template>
	<view class="app-shell">
		<view class="top-wash" aria-hidden="true"></view>

		<view class="page-main">
			<view class="page-header">
				<view>
					<text class="page-title">{{ pageTitle }}</text>
				</view>
				<button class="header-action" hover-class="header-action--pressed" aria-label="消息通知" @click="openMessagesTab">
					<image class="header-action-icon" src="/static/icons/nav/notification.svg" mode="aspectFit" />
					<view class="header-dot"></view>
				</button>
			</view>

			<view class="tab-stage">
			<view v-if="activeTab === 0" key="workbench" class="tab-page" data-testid="panel-workbench">
				<view class="welcome-row">
					<view>
						<text class="welcome-title">早上好，{{ displayName }}</text>
					<text class="welcome-date">{{ currentDate }} · {{ storeName }}</text>
					</view>
				</view>

				<view v-if="metrics.length" class="section-heading">
					<text class="section-title">门店概览</text>
					<text class="section-link">实时数据</text>
				</view>
				<view v-if="metrics.length" class="metric-grid">
					<view v-for="metric in metrics" :key="metric.label" class="metric-card">
						<text class="metric-value">{{ metric.value }}</text>
						<text class="metric-label">{{ metric.label }}</text>
						<text class="metric-trend" :class="{ 'metric-trend--warn': metric.warn }">{{ metric.trend }}</text>
					</view>
				</view>

				<view class="section-heading section-heading--tasks">
					<text class="section-title">今日任务</text>
					<button class="section-link-button" hover-class="link-pressed" @click="goToTasks">查看全部</button>
				</view>
				<view v-if="tasks.length" class="list-card">
					<button v-for="task in tasks.slice(0, 3)" :key="task.id" class="task-row" hover-class="list-row--pressed" @click="openTask(task)">
						<view class="task-status" :class="`task-status--${task.tone}`"></view>
						<view class="task-copy">
							<text class="task-title">{{ task.title }}</text>
							<text class="task-meta">{{ task.owner }} · {{ task.time }}</text>
						</view>
						<text class="task-tag">{{ task.status }}</text>
					</button>
				</view>
				<view v-else class="empty-card"><text>暂无真实任务数据</text></view>
			</view>

			<view v-else-if="activeTab === 1" key="tasks" class="tab-page" data-testid="panel-tasks">
				<view class="task-summary">
					<view><text class="summary-number">{{ tasks.length }}</text><text class="summary-unit">项</text></view>
					<text class="summary-title">待处理任务</text>
					<text class="summary-subtitle">数据以实际任务接口为准</text>
				</view>

				<view class="segment-control">
					<button v-for="(label, index) in taskFilters" :key="label" class="segment-item" :class="{ 'segment-item--active': taskFilter === index }" @click="taskFilter = index">{{ label }}</button>
				</view>

				<view v-if="tasks.length" class="list-card task-list-card">
					<button v-for="task in tasks" :key="task.id" class="task-row task-row--large" hover-class="list-row--pressed" @click="openTask(task)">
						<view class="task-status" :class="`task-status--${task.tone}`"></view>
						<view class="task-copy">
							<view class="task-line"><text class="task-title">{{ task.title }}</text><text class="task-tag">{{ task.status }}</text></view>
							<text class="task-description">{{ task.description }}</text>
							<text class="task-meta">{{ task.owner }} · 截止 {{ task.time }}</text>
						</view>
					</button>
				</view>
				<view v-else class="empty-card"><text>暂无任务</text></view>
			</view>

			<view v-else-if="activeTab === 2" key="messages" class="tab-page" data-testid="panel-messages">
				<view class="notice-card">
					<image class="notice-icon" src="/static/icons/nav/notification.svg" mode="aspectFit" />
					<view class="notice-copy"><text class="notice-title">开启消息通知</text><text class="notice-subtitle">重要经营消息及时提醒，不错过待办事项</text></view>
					<button class="notice-button" hover-class="notice-button--pressed">去设置</button>
				</view>

				<view class="search-box"><image class="search-icon" src="/static/icons/nav/search.svg" mode="aspectFit" /><text class="search-placeholder">搜索通知、任务或运营消息</text></view>
				<view class="message-filters">
					<button v-for="(label, index) in messageFilters" :key="label" class="message-filter" :class="{ 'message-filter--active': messageFilter === index }" @click="messageFilter = index">{{ label }}</button>
				</view>

				<view v-if="messages.length" class="message-list">
					<button v-for="message in messages" :key="message.id" class="message-row" hover-class="list-row--pressed" @click="readMessage(message)">
						<view class="message-avatar" :class="`message-avatar--${message.tone}`"><image :src="message.icon" mode="aspectFit" /></view>
						<view class="message-copy">
							<view class="message-title-line"><text class="message-title">{{ message.title }}</text><text class="message-time">{{ message.time }}</text></view>
							<text class="message-preview">{{ message.preview }}</text>
						</view>
						<view v-if="message.unread" class="message-unread"></view>
					</button>
				</view>
				<view v-else class="empty-card"><text>暂无消息</text></view>
			</view>

			<view v-else key="profile" class="tab-page" data-testid="panel-profile">
				<view class="profile-card">
					<view class="profile-avatar">{{ avatarText }}</view>
					<view class="profile-copy"><text class="profile-name">{{ displayName }}</text><text class="profile-account">{{ username }} · 门店管理员</text></view>
					<image class="profile-arrow" src="/static/icons/nav/right.svg" mode="aspectFit" />
				</view>

				<view class="store-card">
					<text class="store-card-label">当前门店</text>
					<text class="store-card-title">{{ storeName }}</text>
					<text class="store-card-meta">{{ tenantName }}</text>
				</view>

				<view class="settings-card">
					<button v-for="item in profileItems" :key="item.label" class="settings-row" hover-class="list-row--pressed" @click="showPending(item.label)">
						<text class="settings-label">{{ item.label }}</text><text class="settings-value">{{ item.value }}</text><image class="settings-arrow" src="/static/icons/nav/right.svg" mode="aspectFit" />
					</button>
				</view>
				<button class="logout-button" hover-class="logout-button--pressed" @click="handleLogout">退出登录</button>
			</view>
			</view>
		</view>
		<app-tab-bar :active-index="activeTab" @change="setActiveTab" />

	</view>
</template>

<script>
	import { logout } from '@/api/auth.js'
	import { clearSession, getSession } from '@/utils/auth.js'
	import AppTabBar from '@/components/app-tab-bar/app-tab-bar.vue'

	export default {
		components: { AppTabBar },
		data() {
			return {
				activeTab: 0,
				session: null,
				taskFilter: 0,
				messageFilter: 0,
				storeName: '未选择门店',
				taskFilters: ['待处理', '进行中', '已完成'],
				messageFilters: ['全部', '经营提醒', '系统通知'],
				metrics: [],
				tasks: [],
				messages: [],
				profileItems: [
					{ label: '账号与安全', value: '' },
					{ label: '消息设置', value: '已开启' },
					{ label: '帮助与反馈', value: '' },
					{ label: '关于平台', value: 'v1.0.0' }
				]
			}
		},
		computed: {
			pageTitle() { return ['首页', '任务', '应用', '我的'][this.activeTab] },
			currentDate() { return new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date()) },
			tenantName() { return this.session?.user?.tenantName || '当前组织' },
			displayName() { return this.session?.user?.displayName || '当前用户' },
			username() { return this.session?.user?.username || '—' },
			avatarText() { return this.displayName.slice(0, 1) }
		},
		onLoad() {
			this.session = getSession()
			if (!this.session) uni.reLaunch({ url: '/pages/index/index' })
		},
		async onPullDownRefresh() {
			try {
				// 当前页面接入真实接口后，可在这里并行刷新任务、消息和门店概览。
				this.session = getSession()
				await new Promise(resolve => setTimeout(resolve, 450))
			} finally {
				uni.stopPullDownRefresh()
			}
		},
		methods: {
			setActiveTab(index) {
				if (!Number.isInteger(index) || index < 0 || index > 3 || index === this.activeTab) return
				this.activeTab = index
			},
			goToTasks() {
				this.setActiveTab(1)
			},
			openMessagesTab() { this.setActiveTab(2) },
			openTask(task) { uni.showToast({ title: `${task.title} · ${task.status}`, icon: 'none' }) },
			readMessage(message) { message.unread = false; uni.showToast({ title: '已标记为已读', icon: 'none' }) },
			showPending(label) { uni.showToast({ title: `${label}功能建设中`, icon: 'none' }) },
			handleLogout() {
				uni.showModal({
					title: '退出登录',
					content: '确定退出当前账号吗？',
					confirmColor: '#165DFF',
					success: async ({ confirm }) => {
						if (!confirm) return
						try { await logout() } catch (error) { /* 本地会话仍需清理 */ }
						clearSession()
						uni.reLaunch({ url: '/pages/index/index' })
					}
				})
			}
		}
	}
</script>

<style scoped>
	.app-shell {
		--primary: #165dff;
		--text: #1d2129;
		--muted: #86909c;
		--soft: #f2f3f5;
		--line: #e5e6eb;
		position: relative;
		box-sizing: border-box;
		min-height: 100vh;
		overflow-x: hidden;
		background: linear-gradient(180deg, #eaf4ff 0%, #f4f7fc 32%, #f7f8fa 66%, #f7f8fa 100%);
		color: var(--text);
		font-family: Inter, "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
	}

	.top-wash {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		background: linear-gradient(135deg, rgba(232, 243, 255, 0.98), rgba(247, 249, 253, 0.94) 42%, #f7f8fa 76%);
		pointer-events: none;
	}

	.page-main {
		position: relative;
		z-index: 1;
		box-sizing: border-box;
		width: 100%;
		max-width: 820rpx;
		min-height: 100vh;
		margin: 0 auto;
		padding: calc(env(safe-area-inset-top) + 40rpx) 32rpx calc(146rpx + env(safe-area-inset-bottom));
	}

	.page-header { display: flex; align-items: center; justify-content: space-between; }
	.page-title { display: block; margin-top: 12rpx; font-size: 54rpx; font-weight: 650; line-height: 1.2; letter-spacing: -1rpx; }

	.header-action {
		position: relative;
		display: flex;
		align-items: center;
		justify-content: center;
		width: 78rpx;
		height: 78rpx;
		margin: 0;
		padding: 0;
		border: 1rpx solid rgba(229, 230, 235, 0.78);
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.74);
		box-shadow: 0 8rpx 24rpx rgba(29, 33, 41, 0.06);
		backdrop-filter: blur(16px);
	}
	.header-action::after, button::after { border: 0; }
	.header-action--pressed { transform: scale(0.94); }
	.header-action-icon { width: 40rpx; height: 40rpx; }
	.header-dot { position: absolute; top: 8rpx; right: 8rpx; width: 14rpx; height: 14rpx; border: 3rpx solid #fff; border-radius: 50%; background: #f53f3f; }

	.tab-stage { position: relative; margin-top: 50rpx; }
	.tab-page { width: 100%; margin-top: 0; }

	.welcome-row { display: flex; align-items: flex-start; justify-content: space-between; }
	.welcome-title { display: block; font-size: 34rpx; font-weight: 600; line-height: 1.35; }
	.welcome-date { display: block; margin-top: 10rpx; color: var(--muted); font-size: 24rpx; line-height: 1.4; }
	.weather-pill { flex: 0 0 auto; padding: 10rpx 18rpx; border-radius: 999rpx; background: rgba(255,255,255,.72); color: #4e5969; font-size: 22rpx; }

	.focus-card {
		display: flex;
		align-items: flex-end;
		box-sizing: border-box;
		margin-top: 34rpx;
		padding: 30rpx;
		border: 1rpx solid rgba(64, 128, 255, 0.12);
		border-radius: 28rpx;
		background: rgba(232, 243, 255, 0.92);
		box-shadow: 0 12rpx 30rpx rgba(22, 93, 255, 0.06);
	}
	.focus-copy { flex: 1; min-width: 0; }
	.focus-kicker { display: block; color: var(--primary); font-size: 22rpx; font-weight: 600; }
	.focus-title { display: block; margin-top: 12rpx; font-size: 31rpx; font-weight: 600; line-height: 1.35; }
	.focus-subtitle { display: block; margin-top: 10rpx; color: #4e5969; font-size: 23rpx; line-height: 1.55; }
	.focus-button { flex: 0 0 auto; height: 64rpx; margin: 0 0 2rpx 20rpx; padding: 0 24rpx; border: 0; border-radius: 16rpx; background: var(--primary); color: #fff; font-size: 24rpx; font-weight: 500; line-height: 64rpx; }
	.focus-button--pressed { transform: scale(.96); background: #0e42d2; }

	.section-heading { display: flex; align-items: center; justify-content: space-between; margin-top: 44rpx; }
	.section-heading--tasks { margin-top: 48rpx; }
	.section-title { font-size: 30rpx; font-weight: 600; }
	.section-link, .section-link-button { color: var(--primary); font-size: 23rpx; }
	.section-link-button { margin: 0; padding: 8rpx 0; border: 0; background: transparent; line-height: 1; }
	.link-pressed { opacity: .55; }

	.metric-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16rpx; margin-top: 24rpx; }
	.metric-card { min-width: 0; padding: 24rpx 20rpx; border: 1rpx solid rgba(229,230,235,.8); border-radius: 24rpx; background: rgba(255,255,255,.9); }
	.metric-value { display: block; overflow: hidden; font-size: 31rpx; font-weight: 650; line-height: 1.25; text-overflow: ellipsis; white-space: nowrap; }
	.metric-label { display: block; margin-top: 10rpx; color: #4e5969; font-size: 22rpx; }
	.metric-trend { display: block; margin-top: 16rpx; color: #00b42a; font-size: 19rpx; line-height: 1.3; }
	.metric-trend--warn { color: #f77234; }

	.list-card, .settings-card { margin-top: 22rpx; overflow: hidden; border: 1rpx solid rgba(229,230,235,.86); border-radius: 26rpx; background: rgba(255,255,255,.94); }
	.empty-card { margin-top: 22rpx; padding: 56rpx 24rpx; border: 1rpx solid rgba(229,230,235,.86); border-radius: 26rpx; background: #fff; color: var(--muted); font-size: 24rpx; text-align: center; }
	.task-row, .message-row, .settings-row { display: flex; align-items: center; box-sizing: border-box; width: 100%; margin: 0; padding: 25rpx 24rpx; border: 0; border-bottom: 1rpx solid #f2f3f5; border-radius: 0; background: transparent; text-align: left; line-height: 1; }
	.task-row:last-child, .message-row:last-child, .settings-row:last-child { border-bottom: 0; }
	.list-row--pressed { background: #f7f8fa; }
	.task-status { flex: 0 0 auto; width: 14rpx; height: 14rpx; margin-right: 22rpx; border-radius: 50%; }
	.task-status--danger { background: #f53f3f; box-shadow: 0 0 0 7rpx rgba(245,63,63,.09); }
	.task-status--warning { background: #ff7d00; box-shadow: 0 0 0 7rpx rgba(255,125,0,.09); }
	.task-status--primary { background: var(--primary); box-shadow: 0 0 0 7rpx rgba(22,93,255,.09); }
	.task-status--muted { background: #c9cdd4; box-shadow: 0 0 0 7rpx rgba(201,205,212,.15); }
	.task-copy { flex: 1; min-width: 0; }
	.task-title { display: block; overflow: hidden; font-size: 27rpx; font-weight: 550; line-height: 1.3; text-overflow: ellipsis; white-space: nowrap; }
	.task-meta { display: block; margin-top: 10rpx; color: var(--muted); font-size: 21rpx; line-height: 1.3; }
	.task-tag { flex: 0 0 auto; margin-left: 18rpx; padding: 8rpx 13rpx; border-radius: 999rpx; background: var(--soft); color: #4e5969; font-size: 19rpx; line-height: 1; }

	.task-summary { padding: 32rpx; border-radius: 28rpx; background: linear-gradient(135deg, #165dff, #4080ff); box-shadow: 0 18rpx 36rpx rgba(22,93,255,.17); color: #fff; }
	.summary-number { font-size: 64rpx; font-weight: 650; line-height: 1; }
	.summary-unit { margin-left: 8rpx; font-size: 24rpx; }
	.summary-title { display: block; margin-top: 18rpx; font-size: 29rpx; font-weight: 600; }
	.summary-subtitle { display: block; margin-top: 9rpx; color: rgba(255,255,255,.78); font-size: 22rpx; }
	.summary-progress { height: 8rpx; margin-top: 26rpx; overflow: hidden; border-radius: 999rpx; background: rgba(255,255,255,.22); }
	.summary-progress-value { width: 60%; height: 100%; border-radius: inherit; background: #fff; }
	.segment-control { display: flex; margin-top: 30rpx; padding: 7rpx; border-radius: 20rpx; background: #e5e6eb; }
	.segment-item { flex: 1; height: 62rpx; margin: 0; padding: 0; border: 0; border-radius: 16rpx; background: transparent; color: #4e5969; font-size: 23rpx; line-height: 62rpx; }
	.segment-item--active { background: #fff; box-shadow: 0 5rpx 13rpx rgba(29,33,41,.08); color: var(--text); font-weight: 600; }
	.task-list-card { margin-top: 26rpx; }
	.task-row--large { align-items: flex-start; padding-top: 29rpx; padding-bottom: 29rpx; }
	.task-line { display: flex; align-items: center; justify-content: space-between; }
	.task-description { display: block; margin-top: 12rpx; color: #4e5969; font-size: 22rpx; line-height: 1.55; }

	.notice-card { display: flex; align-items: center; padding: 25rpx; border: 1rpx solid rgba(64,128,255,.13); border-radius: 25rpx; background: rgba(232,243,255,.9); }
	.notice-icon { flex: 0 0 auto; width: 42rpx; height: 42rpx; }
	.notice-copy { flex: 1; min-width: 0; margin-left: 18rpx; }
	.notice-title { display: block; font-size: 27rpx; font-weight: 600; }
	.notice-subtitle { display: block; margin-top: 8rpx; color: #4e5969; font-size: 20rpx; line-height: 1.45; }
	.notice-button { flex: 0 0 auto; height: 58rpx; margin: 0 0 0 18rpx; padding: 0 20rpx; border: 0; border-radius: 15rpx; background: var(--primary); color: #fff; font-size: 22rpx; line-height: 58rpx; }
	.notice-button--pressed { transform: scale(.95); }
	.search-box { display: flex; align-items: center; height: 86rpx; margin-top: 28rpx; padding: 0 25rpx; border-radius: 22rpx; background: #fff; color: #c9cdd4; }
	.search-icon { width: 36rpx; height: 36rpx; }
	.search-placeholder { margin-left: 16rpx; font-size: 24rpx; }
	.message-filters { display: flex; gap: 34rpx; margin-top: 30rpx; }
	.message-filter { position: relative; margin: 0; padding: 12rpx 0 18rpx; border: 0; background: transparent; color: #4e5969; font-size: 25rpx; line-height: 1; }
	.message-filter--active { color: var(--text); font-weight: 600; }
	.message-filter--active::before { position: absolute; right: 18%; bottom: 4rpx; left: 18%; height: 5rpx; border-radius: 999rpx; background: var(--primary); content: ''; }
	.message-list { margin-top: 8rpx; overflow: hidden; border-radius: 26rpx; background: rgba(255,255,255,.94); }
	.message-row { position: relative; padding-top: 28rpx; padding-bottom: 28rpx; }
	.message-avatar { display: flex; flex: 0 0 auto; align-items: center; justify-content: center; width: 78rpx; height: 78rpx; border-radius: 24rpx; }
	.message-avatar image { width: 39rpx; height: 39rpx; }
	.message-avatar--warning { background: #fff7e8; }
	.message-avatar--primary { background: #e8f3ff; }
	.message-avatar--success { background: #e8ffea; }
	.message-avatar--muted { background: #f2f3f5; }
	.message-copy { flex: 1; min-width: 0; margin-left: 21rpx; }
	.message-title-line { display: flex; align-items: center; justify-content: space-between; }
	.message-title { font-size: 27rpx; font-weight: 550; }
	.message-time { color: #c9cdd4; font-size: 20rpx; }
	.message-preview { display: block; margin-top: 11rpx; overflow: hidden; color: var(--muted); font-size: 22rpx; line-height: 1.45; text-overflow: ellipsis; white-space: nowrap; }
	.message-unread { position: absolute; top: 26rpx; right: 18rpx; width: 13rpx; height: 13rpx; border-radius: 50%; background: #f53f3f; }

	.profile-card { display: flex; align-items: center; padding: 31rpx; border-radius: 28rpx; background: #fff; }
	.profile-avatar { display: flex; align-items: center; justify-content: center; width: 92rpx; height: 92rpx; border-radius: 50%; background: #e8f3ff; color: var(--primary); font-size: 36rpx; font-weight: 650; }
	.profile-copy { flex: 1; min-width: 0; margin-left: 22rpx; }
	.profile-name { display: block; font-size: 31rpx; font-weight: 600; }
	.profile-account { display: block; margin-top: 10rpx; color: var(--muted); font-size: 22rpx; }
	.profile-arrow { width: 35rpx; height: 35rpx; }
	.store-card { margin-top: 24rpx; padding: 29rpx; border-radius: 26rpx; background: linear-gradient(135deg, #1d2129, #344257); color: #fff; }
	.store-card-label { display: block; color: #a9b3c2; font-size: 21rpx; }
	.store-card-title { display: block; margin-top: 16rpx; font-size: 31rpx; font-weight: 600; }
	.store-card-meta { display: block; margin-top: 11rpx; color: #c9cdd4; font-size: 21rpx; }
	.settings-card { margin-top: 25rpx; }
	.settings-row { min-height: 92rpx; }
	.settings-label { flex: 1; font-size: 25rpx; }
	.settings-value { margin-right: 12rpx; color: var(--muted); font-size: 22rpx; }
	.settings-arrow { width: 30rpx; height: 30rpx; }
	.logout-button { width: 100%; height: 88rpx; margin-top: 28rpx; border: 1rpx solid #ffccc7; border-radius: 22rpx; background: #fff; color: #f53f3f; font-size: 25rpx; font-weight: 500; line-height: 88rpx; }
	.logout-button--pressed { background: #fff1f0; transform: scale(.99); }

	@media screen and (max-width: 360px) {
		.metric-grid { gap: 10rpx; }
		.metric-card { padding: 20rpx 14rpx; }
		.metric-value { font-size: 27rpx; }
		.focus-subtitle { display: none; }
	}

</style>
