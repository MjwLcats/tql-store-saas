<template>
	<view class="tabbar-wrap" aria-label="主导航">
		<view class="tabbar">
			<!-- 胶囊选中背景暂时停用，保留以便后续恢复。
			<view
				class="active-pill"
				:style="pillStyle"
			>
				<view class="active-pill__shine"></view>
			</view>
			-->

			<button
				v-for="(item, index) in tabs"
				:key="item.text"
				class="tab-item"
				:class="{ 'tab-item--active': activeIndex === index }"
				hover-class="tab-item--pressed"
				hover-start-time="0"
				hover-stay-time="80"
				:aria-label="item.text"
				@click="select(index)"
			>
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
				tabs: [
					{ text: '首页' },
					{ text: '任务' },
					{ text: '应用' },
					{ text: '我的' }
				]
			}
		},
		/* 胶囊位移逻辑暂时停用，保留以便后续恢复。
		computed: {
			pillStyle() {
				return { transform: `translate3d(${this.activeIndex * 100}%, 0, 0)` }
			}
		},
		*/
		methods: {
			select(index) {
				if (index === this.activeIndex || !this.tabs[index]) return
				this.$emit('change', index)
			},
		}
	}
</script>

<style scoped>
	.tabbar-wrap {
		position: fixed;
		z-index: 999;
		left: 0;
		right: 0;
		bottom: 0;
		box-sizing: border-box;
		padding-bottom: env(safe-area-inset-bottom);
		border-top: 1rpx solid rgba(0, 0, 0, 0.1);
		background: rgba(250, 250, 250, 0.98);
		box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.025);
		backdrop-filter: blur(20rpx);
		-webkit-backdrop-filter: blur(20rpx);
	}

	.tabbar {
		position: relative;
		display: grid;
		grid-template-columns: repeat(4, minmax(0, 1fr));
		box-sizing: border-box;
		width: 100%;
		height: 96rpx;
		padding: 6rpx 20rpx 4rpx;
		background: transparent;
	}

	/* 胶囊选中背景暂时停用，保留以便后续恢复。
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

	.active-pill__shine {
		position: absolute;
		inset: 4rpx 8rpx auto;
		height: 38%;
		border-radius: 999rpx;
		background: rgba(255, 255, 255, 0.28);
		filter: blur(5rpx);
	}
	*/

	.tab-item {
		position: relative;
		z-index: 2;
		display: flex;
		align-items: center;
		justify-content: center;
		min-width: 0;
		height: 100%;
		margin: 0;
		padding: 0;
		border: 0;
		border-radius: 0;
		background: transparent;
		color: #666a72;
		line-height: 1;
	}

	.tab-item::after { display: none; }

	.tab-label {
		font-size: 28rpx;
		font-weight: 400;
		line-height: 1;
		letter-spacing: 0.2rpx;
		white-space: nowrap;
		transition: color 160ms ease, font-weight 160ms ease, opacity 120ms ease, transform 120ms ease;
	}

	.tab-item--active .tab-label {
		color: #165dff;
		font-weight: 600;
	}

	.tab-item--pressed .tab-label { opacity: 0.68; transform: scale(0.96); }

	@media (prefers-reduced-motion: reduce) {
		.tab-label { transition-duration: 1ms !important; }
	}
</style>
