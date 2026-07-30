<template>
	<view class="inventory-page">
		<view class="top-area">
			<view class="nav-row"><button class="back" @click="back">‹</button><text class="nav-title">盘点任务</text><button class="refresh" @click="load">刷新</button></view>
			<view class="hero">
				<text class="hero-label">当前门店</text>
				<text class="hero-title">{{ storeName }}</text>
				<view class="hero-stats"><view><text class="stat-value">{{ activeCount }}</text><text class="stat-label">进行中</text></view><view><text class="stat-value">{{ pendingCount }}</text><text class="stat-label">待审核</text></view><view><text class="stat-value">{{ tasks.length }}</text><text class="stat-label">全部任务</text></view></view>
			</view>
		</view>
		<view class="content">
			<view class="filter-row"><button v-for="item in filters" :key="item.value" :class="['filter', {active:filter===item.value}]" @click="filter=item.value">{{item.label}}</button></view>
			<view v-if="loading" class="state-card">正在加载盘点任务…</view>
			<view v-else-if="visibleTasks.length===0" class="state-card"><text class="empty-title">暂无盘点任务</text><text class="empty-copy">后台创建并开始盘点后，任务会显示在这里。</text></view>
			<button v-for="task in visibleTasks" :key="task.id" class="task-card" hover-class="task-card--pressed" @click="openTask(task)">
				<view class="task-head"><text class="task-title">{{task.taskName}}</text><text :class="['status',`status--${tone(task.status)}`]">{{statusLabel(task.status)}}</text></view>
				<text class="task-code">{{task.taskCode}}</text>
				<view class="task-meta"><text>{{formatTime(task.plannedStartTime)}}</text><text>至 {{formatTime(task.plannedEndTime)}}</text></view>
				<view class="task-footer"><text>{{task.status==='IN_PROGRESS'?'继续盘点':'查看任务详情'}}</text><text class="arrow">→</text></view>
			</button>
		</view>
	</view>
</template>
<script>
	import { fetchInventoryTasks } from '@/api/inventory.js'
	import { getSession } from '@/utils/auth.js'
	export default {
		data(){return{loading:false,tasks:[],filter:'ALL',filters:[{label:'全部',value:'ALL'},{label:'进行中',value:'IN_PROGRESS'},{label:'待审核',value:'PENDING_REVIEW'}],storeId:null,storeName:'当前门店'}},
		computed:{visibleTasks(){return this.filter==='ALL'?this.tasks:this.tasks.filter(t=>t.status===this.filter)},activeCount(){return this.tasks.filter(t=>t.status==='IN_PROGRESS').length},pendingCount(){return this.tasks.filter(t=>t.status==='PENDING_REVIEW').length}},
		onLoad(options={}){const session=getSession();const user=session&&session.user||{};this.storeId=Number(options.storeId||user.primaryStoreId||user.storeId||(user.storeIds&&user.storeIds[0]));this.storeName=user.primaryStoreName||user.storeName||user.tenantName||'当前门店';this.load()},
		onPullDownRefresh(){this.load().finally(()=>uni.stopPullDownRefresh())},
		methods:{async load(){if(!this.storeId){this.tasks=[];return}this.loading=true;try{this.tasks=await fetchInventoryTasks(this.storeId)||[]}catch(e){this.tasks=[];uni.showToast({title:e.message||'任务加载失败',icon:'none'})}finally{this.loading=false}},openTask(task){uni.navigateTo({url:`/pages/inventory/count?id=${task.id}&storeId=${task.storeId}&name=${encodeURIComponent(task.taskName)}`})},back(){uni.navigateBack()},formatTime(v){return(v||'').replace('T',' ').slice(5,16)},statusLabel(s){return{DRAFT:'待开始',IN_PROGRESS:'盘点中',PENDING_REVIEW:'待审核',REJECTED:'已驳回',APPROVED:'已审核',CLOSED:'已关账'}[s]||s},tone(s){return{IN_PROGRESS:'blue',PENDING_REVIEW:'orange',REJECTED:'red',APPROVED:'green',CLOSED:'gray'}[s]||'gray'}}
	}
</script>
<style scoped>
	.inventory-page{min-height:100vh;background:#f7f8fa;color:#1d2129;font-family:Inter,"PingFang SC","Microsoft YaHei",sans-serif}.top-area{padding:calc(env(safe-area-inset-top) + 18rpx) 32rpx 34rpx;background:linear-gradient(150deg,#e8f3ff,#f5f9ff)}button::after{border:0}.nav-row{display:flex;align-items:center;justify-content:space-between;height:72rpx}.back,.refresh{margin:0;padding:0;border:0;background:transparent;line-height:72rpx}.back{width:60rpx;font-size:52rpx;text-align:left}.refresh{color:#165dff;font-size:24rpx}.nav-title{font-size:31rpx;font-weight:650}.hero{margin-top:28rpx;padding:30rpx;border:1rpx solid rgba(22,93,255,.1);border-radius:28rpx;background:#fff;box-shadow:0 16rpx 42rpx rgba(22,93,255,.08)}.hero-label{display:block;color:#86909c;font-size:22rpx}.hero-title{display:block;margin-top:10rpx;font-size:35rpx;font-weight:650}.hero-stats{display:grid;grid-template-columns:repeat(3,1fr);margin-top:30rpx}.hero-stats view+view{border-left:1rpx solid #e5e6eb}.stat-value,.stat-label{display:block;text-align:center}.stat-value{font-size:36rpx;font-weight:650}.stat-label{margin-top:8rpx;color:#86909c;font-size:21rpx}.content{padding:28rpx 32rpx 60rpx}.filter-row{display:flex;gap:14rpx;margin-bottom:24rpx}.filter{height:58rpx;margin:0;padding:0 24rpx;border:0;border-radius:16rpx;background:#fff;color:#4e5969;font-size:23rpx;line-height:58rpx}.filter.active{background:#165dff;color:#fff}.state-card{padding:80rpx 30rpx;border-radius:24rpx;background:#fff;color:#86909c;font-size:24rpx;text-align:center}.empty-title,.empty-copy{display:block}.empty-title{color:#1d2129;font-size:29rpx;font-weight:600}.empty-copy{margin-top:14rpx;line-height:1.6}.task-card{box-sizing:border-box;width:100%;margin:0 0 20rpx;padding:28rpx;border:1rpx solid #e5e6eb;border-radius:24rpx;background:#fff;text-align:left;line-height:1}.task-card--pressed{transform:scale(.985);background:#f9fafb}.task-head{display:flex;align-items:center;justify-content:space-between}.task-title{font-size:29rpx;font-weight:600}.status{padding:8rpx 14rpx;border-radius:999rpx;font-size:20rpx}.status--blue{background:#e8f3ff;color:#165dff}.status--orange{background:#fff7e8;color:#ff7d00}.status--red{background:#fff1f0;color:#f53f3f}.status--green{background:#e8ffea;color:#00b42a}.status--gray{background:#f2f3f5;color:#86909c}.task-code{display:block;margin-top:12rpx;color:#86909c;font-family:monospace;font-size:21rpx}.task-meta{display:flex;justify-content:space-between;margin-top:24rpx;padding:20rpx;border-radius:18rpx;background:#f7f8fa;color:#4e5969;font-size:21rpx}.task-footer{display:flex;justify-content:space-between;margin-top:24rpx;color:#165dff;font-size:23rpx}.arrow{font-size:28rpx}
</style>
