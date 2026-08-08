<template>
	<view class="tabbar-wrap" aria-label="主导航">
		<view class="tabbar">
			<button v-for="(item, index) in tabs" :key="item.text" class="tab-item" :class="{ 'tab-item--active': activeIndex === index }" :aria-label="item.text" @click="select(index)">
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
					{ text: '分析' },
					{ text: '任务' },
					{ text: '应用' },
					{ text: '我的' }
				]
			}
		},
		methods: {
			select(index) {
				if (index === this.activeIndex || !this.tabs[index]) return
				this.$emit('change', index)
			}
		}
	}
</script>

<style scoped>
	.tabbar-wrap {
		position: fixed;
		z-index: 70;
		left: 0;
		right: 0;
		bottom: 0;
		border-top: 1rpx solid #e5e6eb;
		background: rgba(255, 255, 255, 0.98);
		box-shadow: 0 -4rpx 20rpx rgba(29, 33, 41, 0.05);
		backdrop-filter: blur(20rpx);
		-webkit-backdrop-filter: blur(20rpx);
	}
	.tabbar { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); min-height: 96rpx; padding: 6rpx 12rpx calc(4rpx + env(safe-area-inset-bottom)); background: transparent; }
	.tab-item { display: flex; align-items: center; justify-content: center; min-width: 0; height: 88rpx; margin: 0; padding: 0; border: 0; border-radius: 0; background: transparent; color: #6b7785; line-height: 1; }
	.tab-item::after { display: none; }
	.tab-label { font-size: 27rpx; font-weight: 500; line-height: 1; white-space: nowrap; }
	.tab-item--active { color: #165dff; }
	.tab-item--active .tab-label { font-weight: 650; }
</style>
