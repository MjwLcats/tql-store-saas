<template>
	<view class="login-page">
		<view class="ambient ambient--top" aria-hidden="true"></view>

		<view class="login-main" role="main">
			<view class="brand-lockup" aria-label="同庆楼门店运营平台">
				<text class="brand-name">同庆楼</text>
				<view class="brand-divider" aria-hidden="true"></view>
				<text class="brand-product">门店运营平台</text>
			</view>

			<view class="login-panel">
				<view class="welcome-block">
					<text class="welcome-title">欢迎登录</text>
					<text class="welcome-subtitle">同庆楼门店运营平台</text>
				</view>

				<view class="login-form">
					<view
						class="field"
						:class="{ 'field--active': activeField === 'merchantNo' }"
					>
						<image class="field-icon" src="/static/icons/merchant.svg" mode="aspectFit" />
						<input
							v-model="merchantNo"
							class="field-input"
							data-testid="merchant-no-input"
							type="text"
							maxlength="64"
							placeholder="请输入商户号"
							placeholder-class="field-placeholder"
							confirm-type="next"
							@focus="activeField = 'merchantNo'"
							@blur="activeField = ''"
						/>
					</view>

					<view
						class="field"
						:class="{ 'field--active': activeField === 'account' }"
					>
						<image class="field-icon" src="/static/icons/user.svg" mode="aspectFit" />
						<input
							v-model="account"
							class="field-input"
							data-testid="account-input"
							type="text"
							maxlength="64"
							placeholder="请输入用户名"
							placeholder-class="field-placeholder"
							confirm-type="next"
							@focus="activeField = 'account'"
							@blur="activeField = ''"
						/>
					</view>

					<view
						class="field"
						:class="{ 'field--active': activeField === 'password' }"
					>
						<image class="field-icon" src="/static/icons/lock.svg" mode="aspectFit" />
						<input
							v-model="password"
							class="field-input"
							type="text"
							:password="!passwordVisible"
							maxlength="32"
							placeholder="请输入密码"
							placeholder-class="field-placeholder"
							confirm-type="done"
							@focus="activeField = 'password'"
							@blur="activeField = ''"
							@confirm="handleLogin"
						/>
						<button
							class="field-action field-action--icon"
							data-testid="password-visibility"
							hover-class="field-action--pressed"
							:aria-label="passwordVisible ? '隐藏密码' : '显示密码'"
							@click="passwordVisible = !passwordVisible"
						>
							<image
								class="eye-icon"
								:src="passwordVisible ? '/static/icons/eye-off.svg' : '/static/icons/eye.svg'"
								mode="aspectFit"
							/>
						</button>
					</view>

					<view class="assist-row">
						<button class="text-button" hover-class="text-button--pressed" @click="handleForgot">
							忘记密码
						</button>
					</view>

					<button
						class="login-button"
						data-testid="login-button"
						hover-class="login-button--pressed"
						:loading="loading"
						:disabled="loading"
						@click="handleLogin"
					>
						{{ loading ? '登录中' : '登录' }}
					</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { login } from '@/api/auth.js'
	import { getSession, saveSession } from '@/utils/auth.js'

	export default {
		data() {
			return {
				activeField: '',
				merchantNo: '',
				account: '',
				password: '',
				passwordVisible: false,
				loading: false
			}
		},
		onLoad() {
			if (getSession()) uni.reLaunch({ url: '/pages/home/index' })
		},
		methods: {
			async handleLogin() {
				if (this.loading) return
				if (!this.merchantNo.trim()) {
					uni.showToast({ title: '请输入商户号', icon: 'none' })
					return
				}
				if (!this.account.trim()) {
					uni.showToast({ title: '请输入用户名', icon: 'none' })
					return
				}
				if (!this.password) {
					uni.showToast({ title: '请输入密码', icon: 'none' })
					return
				}

				this.loading = true
				try {
					const result = await login(this.merchantNo, this.account, this.password)
					saveSession(result)
					const displayName = result.user && result.user.displayName
					uni.showToast({
						title: displayName ? `欢迎回来，${displayName}` : '登录成功',
						icon: 'success',
						duration: 900
					})
					uni.$emit('auth:login', result)
					setTimeout(() => uni.reLaunch({ url: '/pages/home/index' }), 420)
				} catch (error) {
					uni.showToast({
						title: error && error.message ? error.message : '登录失败，请稍后重试',
						icon: 'none'
					})
				} finally {
					this.loading = false
				}
			},
			handleForgot() {
				uni.showToast({ title: '找回功能待接入', icon: 'none' })
			}
		}
	}
</script>

<style scoped>
	.login-page {
		--primary: #165dff;
		--primary-hover: #4080ff;
		--primary-pressed: #0e42d2;
		--text: #1d2129;
		--muted: #86909c;
		--placeholder: #c9cdd4;
		--border: #e5e6eb;
		position: relative;
		box-sizing: border-box;
		min-height: 100vh;
		overflow: hidden;
		background: #ffffff;
		color: var(--text);
		font-family: Inter, "PingFang SC", "Microsoft YaHei", system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
	}

	.ambient {
		position: absolute;
		pointer-events: none;
	}

	.ambient--top {
		top: -230rpx;
		right: -300rpx;
		width: 900rpx;
		height: 700rpx;
		background: radial-gradient(circle at 72% 18%, rgba(64, 128, 255, 0.13), rgba(64, 128, 255, 0.045) 38%, rgba(255, 255, 255, 0) 72%);
	}

	.login-main {
		position: relative;
		z-index: 1;
		box-sizing: border-box;
		width: 100%;
		max-width: 820rpx;
		min-height: 100vh;
		margin: 0 auto;
		padding: calc(env(safe-area-inset-top) + 88rpx) 64rpx calc(env(safe-area-inset-bottom) + 88rpx);
	}

	.brand-lockup {
		display: flex;
		align-items: center;
		height: 44rpx;
	}

	.brand-name {
		color: var(--text);
		font-size: 34rpx;
		font-weight: 600;
		line-height: 44rpx;
		letter-spacing: 0;
	}

	.brand-divider {
		width: 2rpx;
		height: 28rpx;
		margin: 0 20rpx;
		background: var(--border);
	}

	.brand-product {
		color: var(--muted);
		font-size: 27rpx;
		font-weight: 400;
		line-height: 44rpx;
	}

	.login-panel {
		margin-top: 160rpx;
	}

	.welcome-block {
		display: flex;
		flex-direction: column;
		align-items: flex-start;
	}

	.welcome-title {
		display: block;
		color: var(--text);
		font-size: 58rpx;
		font-weight: 600;
		line-height: 1.22;
		letter-spacing: -1rpx;
	}

	.welcome-subtitle {
		display: block;
		margin-top: 18rpx;
		color: var(--muted);
		font-size: 28rpx;
		font-weight: 400;
		line-height: 1.5;
	}

	.login-form {
		margin-top: 68rpx;
	}

	.field {
		display: flex;
		align-items: center;
		box-sizing: border-box;
		height: 108rpx;
		margin-bottom: 28rpx;
		padding: 0 30rpx;
		border: 2rpx solid var(--border);
		border-radius: 20rpx;
		background: rgba(255, 255, 255, 0.94);
		transition: border-color 180ms ease, box-shadow 180ms ease, background-color 180ms ease;
	}

	.field--active {
		border-color: var(--primary-hover);
		background: #ffffff;
		box-shadow: 0 0 0 5rpx rgba(64, 128, 255, 0.07);
	}

	.field-icon {
		flex: 0 0 auto;
		width: 40rpx;
		height: 40rpx;
		margin-right: 24rpx;
	}

	.field-input {
		flex: 1;
		min-width: 0;
		height: 100%;
		color: var(--text);
		font-size: 30rpx;
		font-weight: 400;
		line-height: 108rpx;
	}

	.field-placeholder {
		color: var(--placeholder);
		font-size: 30rpx;
	}

	.field-action,
	.text-button {
		box-sizing: border-box;
		margin: 0;
		padding: 0;
		border: 0;
		background: transparent;
		line-height: 1;
	}

	.field-action::after,
	.text-button::after {
		border: 0;
	}

	.field-action--icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 64rpx;
		height: 72rpx;
		margin-right: -12rpx;
	}

	.eye-icon {
		width: 42rpx;
		height: 42rpx;
	}

	.field-action--pressed,
	.text-button--pressed {
		opacity: 0.58;
	}

	.assist-row {
		display: flex;
		justify-content: flex-end;
		margin-top: -8rpx;
	}

	.text-button {
		color: var(--primary);
		font-size: 27rpx;
		font-weight: 500;
		line-height: 56rpx;
	}

	.login-button {
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
		width: 100%;
		height: 100rpx;
		margin-top: 44rpx;
		padding: 0;
		border-radius: 20rpx;
		background: var(--primary);
		box-shadow: 0 8rpx 20rpx rgba(22, 93, 255, 0.1);
		color: #ffffff;
		font-size: 31rpx;
		font-weight: 500;
		line-height: 100rpx;
		letter-spacing: 2rpx;
		transition: transform 150ms ease, box-shadow 150ms ease, background-color 150ms ease;
	}

	.login-button::after {
		border: 0;
	}

	.login-button--pressed {
		transform: translateY(2rpx) scale(0.992);
		background: var(--primary-pressed);
		box-shadow: 0 4rpx 12rpx rgba(22, 93, 255, 0.1);
	}

	.login-button[disabled] {
		background: #94bfff;
		box-shadow: none;
		opacity: 1;
	}

	@media screen and (max-height: 720px) {
		.login-main {
			padding-top: calc(env(safe-area-inset-top) + 60rpx);
		}

		.login-panel {
			margin-top: 100rpx;
		}

		.login-form {
			margin-top: 48rpx;
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.field,
		.login-button {
			transition: none;
		}
	}
</style>
