<template>
	<view class="tabbar-wrap" aria-label="主导航">
		<view class="tabbar">
			<view
				class="active-pill"
				:class="[
					{ 'active-pill--moving': moving },
					`active-pill--${direction}`
				]"
				:style="pillStyle"
			>
				<view class="active-pill__shine"></view>
			</view>

			<button
				v-for="(item, index) in tabs"
				:key="item.url"
				class="tab-item"
				:class="{ 'tab-item--active': visualIndex === index }"
				hover-class="none"
				:aria-label="item.text"
				@click="navigate(index)"
			>
				<view class="tab-icon-wrap">
					<image
						class="tab-icon tab-icon--idle"
						:src="item.icon"
						mode="aspectFit"
					/>
					<image
						class="tab-icon tab-icon--active"
						:src="item.selectedIcon"
						mode="aspectFit"
					/>
					<text v-if="item.badge" class="tab-badge">{{ item.badge }}</text>
					<view v-else-if="item.dot" class="tab-dot"></view>
				</view>
				<text class="tab-label">{{ item.text }}</text>
			</button>
		</view>
	</view>
</template>

<script>
	export default {
		name: 'AppTabBar',
		props: {
			activeIndex: { type: Number, default: 0 }
		},
		data() {
			return {
				visualIndex: this.activeIndex,
				moving: false,
				direction: 'right',
				navigationTimer: null,
				tabs: [
					{ text: '工作台', url: '/pages/home/index', icon: '/static/icons/tabbar/workbench.png', selectedIcon: '/static/icons/tabbar/workbench-selected.png' },
					{ text: '任务', url: '/pages/tasks/index', icon: '/static/icons/tabbar/tasks.png', selectedIcon: '/static/icons/tabbar/tasks-selected.png', badge: '8' },
					{ text: '消息', url: '/pages/messages/index', icon: '/static/icons/tabbar/messages.png', selectedIcon: '/static/icons/tabbar/messages-selected.png', badge: '3' },
					{ text: '我的', url: '/pages/profile/index', icon: '/static/icons/tabbar/profile.png', selectedIcon: '/static/icons/tabbar/profile-selected.png', dot: true }
				]
			}
		},
		computed: {
			pillStyle() {
				return { transform: `translate3d(${this.visualIndex * 100}%, 0, 0)` }
			}
		},
		watch: {
			activeIndex(index) {
				if (!this.moving) this.visualIndex = index
			}
		},
		mounted() {
			uni.hideTabBar({ animation: false })
		},
		beforeDestroy() {
			if (this.navigationTimer) clearTimeout(this.navigationTimer)
		},
		methods: {
			sync(index) {
				if (this.navigationTimer) {
					clearTimeout(this.navigationTimer)
					this.navigationTimer = null
				}
				this.moving = false
				this.visualIndex = index
			},
			navigate(index) {
				if (index === this.visualIndex || !this.tabs[index]) return
				if (this.navigationTimer) {
					clearTimeout(this.navigationTimer)
					this.navigationTimer = null
				}
				this.direction = index > this.visualIndex ? 'right' : 'left'
				this.moving = true
				this.visualIndex = index

				this.navigationTimer = setTimeout(() => {
					uni.switchTab({
						url: this.tabs[index].url,
						fail: () => { this.moving = false }
					})
				}, 220)
			},
		}
	}
</script>

<style scoped>
	.tabbar-wrap {
		position: fixed;
		z-index: 999;
		left: max(24rpx, env(safe-area-inset-left));
		right: max(24rpx, env(safe-area-inset-right));
		bottom: calc(14rpx + env(safe-area-inset-bottom));
		box-sizing: border-box;
		filter: drop-shadow(0 14rpx 34rpx rgba(15, 23, 42, 0.16));
	}

	.tabbar {
		position: relative;
		display: grid;
		grid-template-columns: repeat(4, minmax(0, 1fr));
		box-sizing: border-box;
		width: 100%;
		height: clamp(104rpx, 7.4vh, 124rpx);
		padding: 8rpx;
		overflow: hidden;
		border: 1rpx solid rgba(255, 255, 255, 0.9);
		border-radius: 999rpx;
		background: rgba(255, 255, 255, 0.88);
		box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.92);
		backdrop-filter: blur(26rpx) saturate(145%);
		-webkit-backdrop-filter: blur(26rpx) saturate(145%);
	}

	.active-pill {
		position: absolute;
		z-index: 0;
		top: 8rpx;
		bottom: 8rpx;
		left: 8rpx;
		width: calc((100% - 16rpx) / 4);
		border-radius: 999rpx;
		background: rgba(118, 118, 128, 0.12);
		box-shadow: inset 0 1rpx 1rpx rgba(255, 255, 255, 0.78), 0 4rpx 12rpx rgba(15, 23, 42, 0.05);
		transition: transform 390ms cubic-bezier(0.22, 1.28, 0.36, 1), border-radius 180ms ease;
		will-change: transform;
	}

	.active-pill::after {
		content: '';
		position: absolute;
		top: 10%;
		bottom: 10%;
		width: 42%;
		opacity: 0;
		border-radius: 999rpx;
		background: rgba(118, 118, 128, 0.1);
		filter: blur(8rpx);
		transition: opacity 110ms ease, transform 390ms cubic-bezier(0.22, 1.18, 0.36, 1);
	}

	.active-pill--right::after { right: 72%; transform: scaleX(0.4); transform-origin: right center; }
	.active-pill--left::after { left: 72%; transform: scaleX(0.4); transform-origin: left center; }
	.active-pill--moving::after { opacity: 1; transform: scaleX(1.65); }

	.active-pill__shine {
		position: absolute;
		inset: 4rpx 8rpx auto;
		height: 38%;
		border-radius: 999rpx;
		background: rgba(255, 255, 255, 0.28);
		filter: blur(5rpx);
	}

	.tab-item {
		position: relative;
		z-index: 2;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		min-width: 0;
		height: 100%;
		margin: 0;
		padding: 5rpx 0 4rpx;
		border: 0;
		border-radius: 999rpx;
		background: transparent;
		color: #8e8e93;
		line-height: 1;
	}

	.tab-item::after { display: none; }

	.tab-icon-wrap {
		position: relative;
		width: 40rpx;
		height: 40rpx;
		transition: transform 390ms cubic-bezier(0.2, 1.45, 0.36, 1);
	}

	.tab-icon {
		position: absolute;
		inset: 0;
		width: 40rpx;
		height: 40rpx;
		transition: opacity 180ms ease, transform 390ms cubic-bezier(0.2, 1.45, 0.36, 1);
	}

	.tab-icon--active { opacity: 0; transform: scale(0.72); }
	.tab-item--active .tab-icon-wrap { transform: translateY(-1rpx) scale(1.07); }
	.tab-item--active .tab-icon--idle { opacity: 0; transform: scale(0.76); }
	.tab-item--active .tab-icon--active { opacity: 1; transform: scale(1); }

	.tab-label {
		margin-top: 3rpx;
		font-size: clamp(20rpx, 2.7vw, 24rpx);
		font-weight: 500;
		line-height: 1.05;
		white-space: nowrap;
		transition: color 180ms ease, transform 390ms cubic-bezier(0.2, 1.35, 0.36, 1), font-weight 180ms ease;
	}

	.tab-item--active .tab-label {
		color: #165dff;
		font-weight: 600;
		transform: translateY(-1rpx);
	}

	.tab-badge,
	.tab-dot {
		position: absolute;
		z-index: 3;
		top: -10rpx;
		right: -18rpx;
		box-sizing: border-box;
		border: 3rpx solid #fff;
		background: #ff3b30;
	}

	.tab-badge {
		min-width: 34rpx;
		height: 34rpx;
		padding: 0 8rpx;
		border-radius: 18rpx;
		color: #fff;
		font-size: 20rpx;
		font-weight: 700;
		line-height: 28rpx;
		text-align: center;
	}

	.tab-dot {
		top: -7rpx;
		right: -10rpx;
		width: 20rpx;
		height: 20rpx;
		border-radius: 50%;
	}

	@media (prefers-reduced-motion: reduce) {
		.active-pill,
		.active-pill::after,
		.tab-icon-wrap,
		.tab-icon,
		.tab-label { transition-duration: 1ms !important; }
	}
</style>
