<template>
	<view class="count-page">
		<view class="nav"><button @click="back">‹</button><view><text class="title">{{ taskName }}</text><text class="subtitle">盘点录入 · 本地服务</text></view><text class="progress">{{ completed }}/{{ materials.length }}</text></view>
		<view class="search"><text>⌕</text><input v-model="keyword" placeholder="搜索物料名称或编码" /></view>
		<view class="notice">数据只提交到当前电脑运行的 SaaS 本地数据库，不连接原盘点系统或线上数据库。</view>
		<view v-if="loading" class="state">正在加载盘点物料…</view>
		<view v-else-if="!materials.length" class="state">暂无可盘点物料，请先在本地后台维护物料。</view>
		<view v-else class="material-list">
			<view v-for="item in filtered" :key="item.snapshotId" class="material-card">
				<view><text class="material-name">{{ item.materialName }}</text><text class="material-meta">{{ item.materialCode }} · {{ item.locationName }} · 账面 {{ item.bookQuantity || 0 }}</text><text v-if="String(item.quantity).trim()!==''" :class="['difference',Number(item.quantity)-Number(item.bookQuantity||0)===0?'difference--ok':'difference--warn']">差异 {{ formatDifference(item) }} {{ item.unitName }}</text></view>
				<view class="quantity"><button @click="change(item,-1)">−</button><input v-model="item.quantity" type="digit" placeholder="0" @input="saveDraft" /><text>{{ item.unitName }}</text><button @click="change(item,1)">＋</button></view>
			</view>
		</view>
		<view class="bottom-bar"><view><text class="saved">本机草稿</text><text class="saved-time">{{ savedTime }}</text></view><button class="submit" :disabled="submitting || completed !== materials.length" @click="submit">{{ submitting ? '提交中…' : '提交盘点' }}</button></view>
	</view>
</template>
<script>
	import { startInventoryTask, submitInventoryCounts } from '@/api/inventory.js'
	export default {
		data(){return{taskId:null,taskName:'盘点任务',keyword:'',savedTime:'尚未保存',materials:[],loading:false,submitting:false}},
		computed:{filtered(){const k=this.keyword.trim().toLowerCase();return k?this.materials.filter(i=>`${i.materialCode}${i.materialName}`.toLowerCase().includes(k)):this.materials},completed(){return this.materials.filter(i=>String(i.quantity).trim()!=='').length}},
		onLoad(options={}){this.taskId=Number(options.id);this.taskName=decodeURIComponent(options.name||'盘点任务');this.load()},
		methods:{
			back(){uni.navigateBack()},
			async load(){if(!this.taskId)return;this.loading=true;try{const rows=await startInventoryTask(this.taskId)||[];const saved=uni.getStorageSync(`inventory-draft:${this.taskId}`);const draft=new Map((saved&&saved.materials||[]).map(i=>[i.snapshotId,i.quantity]));this.materials=rows.map(i=>({...i,quantity:draft.has(i.snapshotId)?draft.get(i.snapshotId):(i.countedQuantity==null?'':String(i.countedQuantity))}));if(saved)this.savedTime=saved.savedTime||'已恢复草稿'}catch(e){uni.showToast({title:e.message||'盘点物料加载失败',icon:'none'})}finally{this.loading=false}},
			change(item,delta){item.quantity=String(Math.max(0,Number(item.quantity||0)+delta));this.saveDraft()},
			saveDraft(){this.savedTime=new Date().toLocaleTimeString('zh-CN',{hour:'2-digit',minute:'2-digit'});uni.setStorageSync(`inventory-draft:${this.taskId}`,{materials:this.materials.map(i=>({snapshotId:i.snapshotId,quantity:i.quantity})),savedTime:this.savedTime})},
			formatDifference(item){const value=Number(item.quantity||0)-Number(item.bookQuantity||0);return value>0?`+${value}`:String(value)},
			submit(){if(this.completed!==this.materials.length)return;uni.showModal({title:'确认提交盘点？',content:'提交后任务进入待审核，数据仅写入本地 SaaS 数据库。',confirmColor:'#165DFF',success:async result=>{if(!result.confirm)return;this.submitting=true;try{const key=`local-${this.taskId}-${Date.now()}`;await submitInventoryCounts(this.taskId,this.materials.map(i=>({snapshotId:i.snapshotId,countedQuantity:Number(i.quantity)})),key);uni.removeStorageSync(`inventory-draft:${this.taskId}`);uni.showToast({title:'盘点已提交',icon:'success'});setTimeout(()=>uni.navigateBack(),800)}catch(e){uni.showToast({title:e.message||'提交失败',icon:'none'})}finally{this.submitting=false}}})}
		}
	}
</script>
<style scoped>
	.count-page{box-sizing:border-box;min-height:100vh;padding:calc(env(safe-area-inset-top) + 18rpx) 30rpx 170rpx;background:#f7f8fa;color:#1d2129;font-family:Inter,"PingFang SC","Microsoft YaHei",sans-serif}button::after{border:0}.nav{display:flex;align-items:center;height:84rpx}.nav button{width:58rpx;margin:0;padding:0;border:0;background:transparent;font-size:52rpx;text-align:left}.nav view{flex:1;min-width:0}.title,.subtitle{display:block}.title{overflow:hidden;font-size:30rpx;font-weight:650;text-overflow:ellipsis;white-space:nowrap}.subtitle{margin-top:6rpx;color:#86909c;font-size:20rpx}.progress{color:#165dff;font-size:25rpx;font-weight:600}.search{display:flex;align-items:center;height:78rpx;margin-top:24rpx;padding:0 24rpx;border-radius:20rpx;background:#fff;color:#86909c}.search input{flex:1;margin-left:14rpx;font-size:24rpx}.notice{margin-top:18rpx;padding:18rpx 22rpx;border-radius:18rpx;background:#e8f3ff;color:#4e5969;font-size:21rpx;line-height:1.55}.state{margin-top:22rpx;padding:80rpx 24rpx;border-radius:24rpx;background:#fff;color:#86909c;text-align:center}.material-list{margin-top:22rpx}.material-card{padding:26rpx;margin-bottom:18rpx;border:1rpx solid #e5e6eb;border-radius:24rpx;background:#fff}.material-name,.material-meta,.difference{display:block}.material-name{font-size:28rpx;font-weight:600}.material-meta{margin-top:10rpx;color:#86909c;font-size:21rpx}.difference{margin-top:10rpx;font-size:22rpx;font-weight:600}.difference--warn{color:#f53f3f}.difference--ok{color:#00b42a}.quantity{display:flex;align-items:center;margin-top:24rpx;padding-top:22rpx;border-top:1rpx solid #f2f3f5}.quantity button{width:58rpx;height:58rpx;margin:0;border:0;border-radius:16rpx;background:#f2f3f5;color:#165dff;font-size:28rpx;line-height:58rpx}.quantity input{flex:1;height:58rpx;margin:0 10rpx;border-bottom:2rpx solid #165dff;font-size:32rpx;font-weight:650;text-align:center}.quantity text{margin-right:16rpx;color:#4e5969;font-size:22rpx}.bottom-bar{position:fixed;right:0;bottom:0;left:0;display:flex;align-items:center;justify-content:space-between;padding:22rpx 30rpx calc(22rpx + env(safe-area-inset-bottom));border-top:1rpx solid #e5e6eb;background:rgba(255,255,255,.96)}.saved,.saved-time{display:block}.saved{font-size:23rpx;font-weight:600}.saved-time{margin-top:6rpx;color:#86909c;font-size:19rpx}.submit{height:74rpx;margin:0;padding:0 34rpx;border:0;border-radius:18rpx;background:#165dff;color:#fff;font-size:25rpx;line-height:74rpx}.submit[disabled]{background:#94bfff;color:#fff}
</style>
