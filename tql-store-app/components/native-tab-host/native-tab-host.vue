<template>
	<home-screen ref="screen" :initial-tab="tabIndex" :initial-session="session" />
</template>

<script>
	import HomeScreen from '@/pages/home/index.vue'
	import { getSession } from '@/utils/auth.js'

	export default {
		name: 'NativeTabHost',
		components: { HomeScreen },
		props: {
			tabIndex: { type: Number, required: true }
		},
		data() {
			return { session: getSession() }
		},
		onShow() {
			uni.hideTabBar({ animation: false })
			this.$nextTick(() => {
				if (this.$refs.screen) this.$refs.screen.syncTabBar()
			})
		},
		mounted() {
			if (!this.session) {
				uni.reLaunch({ url: '/pages/index/index' })
			}
		}
	}
</script>
