<template>
  <div class="cost-page">
    <header class="cost-header"><div><h2>盘点管理</h2><p>按门店创建盘点任务，统一跟踪提交、审核和关账进度。</p></div><a-button type="primary" :disabled="!storeId" @click="visible=true"><template #icon><IconPlus /></template>新建盘点</a-button></header>
    <a-card class="cost-panel">
      <div class="panel-toolbar">
        <a-space><span>当前门店</span><a-select v-model="storeId" placeholder="请选择门店" style="width:240px" @change="load"><a-option v-for="store in stores" :key="store.id" :value="store.id">{{ store.name || store.storeName }}</a-option></a-select></a-space>
        <a-input-search v-model="keyword" allow-clear placeholder="搜索任务名称或编号" style="width:260px" />
      </div>
      <a-table :columns="columns" :data="filteredTasks" :loading="loading" row-key="id" :pagination="{ pageSize: 10 }">
        <template #taskCode="{ record }"><span class="source-label">{{ record.taskCode }}</span></template>
        <template #status="{ record }"><a-tag :color="statusInfo(record.status).color">{{ statusInfo(record.status).label }}</a-tag></template>
        <template #time="{ record }"><div>{{ formatTime(record.plannedStartTime) }}</div><small>至 {{ formatTime(record.plannedEndTime) }}</small></template>
        <template #actions="{ record }"><a-space><a-link @click="openDetail(record)">查看差异</a-link><a-link v-if="record.status==='PENDING_REVIEW'" status="success" @click="approve(record)">审核通过</a-link><a-link v-if="record.status==='PENDING_REVIEW'" status="danger" @click="reject(record)">驳回</a-link><a-link v-if="record.status==='APPROVED'" @click="closeTask(record)">关账</a-link></a-space></template>
        <template #empty><a-empty :description="storeId ? '当前门店暂无盘点任务' : '请先选择门店'" /></template>
      </a-table>
    </a-card>
    <a-modal v-model:visible="visible" title="新建盘点任务" :ok-loading="saving" @ok="save">
      <a-form :model="form" layout="vertical">
        <a-form-item label="任务名称" required><a-input v-model="form.taskName" placeholder="例如 7月月末全盘" /></a-form-item>
        <a-form-item label="盘点时间" required><a-range-picker v-model="form.range" show-time value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" /></a-form-item>
        <a-form-item label="备注"><a-textarea v-model="form.remark" :max-length="500" show-word-limit /></a-form-item>
      </a-form>
    </a-modal>
    <a-drawer v-model:visible="detailVisible" title="盘点明细与差异" :width="760" :footer="false">
      <a-descriptions v-if="currentTask" :column="2" bordered size="small">
        <a-descriptions-item label="任务">{{currentTask.taskName}}</a-descriptions-item>
        <a-descriptions-item label="状态"><a-tag :color="statusInfo(currentTask.status).color">{{statusInfo(currentTask.status).label}}</a-tag></a-descriptions-item>
        <a-descriptions-item label="编号">{{currentTask.taskCode}}</a-descriptions-item>
        <a-descriptions-item label="完成度">{{detailItems.filter(i=>i.countedQuantity!=null).length}} / {{detailItems.length}}</a-descriptions-item>
      </a-descriptions>
      <a-table style="margin-top:18px" :data="detailItems" :columns="detailColumns" row-key="snapshotId" :pagination="false">
        <template #material="{record}"><strong>{{record.materialName}}</strong><div class="source-label">{{record.materialCode}} · {{record.locationName}}</div></template>
        <template #book="{record}">{{record.bookQuantity ?? 0}} {{record.unitName}}</template>
        <template #count="{record}">{{record.countedQuantity ?? '未盘'}} <span v-if="record.countedQuantity!=null">{{record.unitName}}</span></template>
        <template #difference="{record}"><span :class="difference(record)>0?'diff-up':difference(record)<0?'diff-down':''">{{record.countedQuantity==null?'—':difference(record)}}</span></template>
      </a-table>
    </a-drawer>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Message, type TableColumnData } from '@arco-design/web-vue';
import { IconPlus } from '@arco-design/web-vue/es/icon';
import { approveInventoryTask, closeInventoryTask, createInventoryTask, fetchInventoryTaskItems, fetchInventoryTasks, fetchStores, rejectInventoryTask, type InventoryCountItem, type InventoryTask } from '@tql-store/api';
import '../styles/cost-workspace.css';
const stores=ref<any[]>([]),storeId=ref<number>(),tasks=ref<InventoryTask[]>([]),keyword=ref(''),loading=ref(false),visible=ref(false),saving=ref(false);
const detailVisible=ref(false),detailItems=ref<InventoryCountItem[]>([]),currentTask=ref<InventoryTask>();
const form=reactive({taskName:'',range:[] as string[],remark:''});
const columns:TableColumnData[]=[{title:'任务编号',slotName:'taskCode',width:170},{title:'任务名称',dataIndex:'taskName'},{title:'计划时间',slotName:'time',width:210},{title:'状态',slotName:'status',width:120},{title:'操作',slotName:'actions',width:280}];
const detailColumns:TableColumnData[]=[{title:'物料',slotName:'material'},{title:'账面数量',slotName:'book',width:130},{title:'实盘数量',slotName:'count',width:130},{title:'差异',slotName:'difference',width:100}];
const filteredTasks=computed(()=>{const key=keyword.value.trim().toLowerCase();return key?tasks.value.filter(item=>`${item.taskCode}${item.taskName}`.toLowerCase().includes(key)):tasks.value});
const preview=import.meta.env.DEV&&new URLSearchParams(window.location.search).get('preview')==='1';
onMounted(async()=>{if(preview){stores.value=[{id:1,storeName:'同庆楼 · 高新店'}];storeId.value=1;tasks.value=[{id:1,storeId:1,taskCode:'PD-20260729-001',taskName:'7月月末原料全盘',status:'IN_PROGRESS',plannedStartTime:'2026-07-29T21:00:00',plannedEndTime:'2026-07-29T23:30:00',version:0},{id:2,storeId:1,taskCode:'PD-20260728-002',taskName:'后厨调料库复盘',status:'PENDING_REVIEW',plannedStartTime:'2026-07-28T20:00:00',plannedEndTime:'2026-07-28T22:00:00',version:1}];return}try{stores.value=await fetchStores();if(stores.value.length){storeId.value=stores.value[0].id;await load()}}catch(e){Message.error(e instanceof Error?e.message:'门店加载失败')}});
async function load(){if(!storeId.value)return;loading.value=true;try{tasks.value=await fetchInventoryTasks(storeId.value)}catch(e){tasks.value=[];Message.error(e instanceof Error?e.message:'盘点任务加载失败')}finally{loading.value=false}}
async function save(){if(!storeId.value||!form.taskName.trim()||form.range.length!==2)return Message.warning('请完整填写任务名称和盘点时间');saving.value=true;try{await createInventoryTask({storeId:storeId.value,taskName:form.taskName,plannedStartTime:form.range[0],plannedEndTime:form.range[1],remark:form.remark||undefined});visible.value=false;Object.assign(form,{taskName:'',range:[],remark:''});Message.success('盘点任务创建成功');await load()}catch(e){Message.error(e instanceof Error?e.message:'创建失败')}finally{saving.value=false}}
async function openDetail(record:InventoryTask){currentTask.value=record;try{detailItems.value=preview?[
  {snapshotId:1,materialCode:'MAT-001',materialName:'东北珍珠米',locationName:'主食库',unitName:'千克',bookQuantity:86,countedQuantity:82.5},
  {snapshotId:2,materialCode:'MAT-002',materialName:'食用油',locationName:'调料库',unitName:'升',bookQuantity:40,countedQuantity:42},
  {snapshotId:3,materialCode:'MAT-003',materialName:'食用盐',locationName:'调料库',unitName:'千克',bookQuantity:12,countedQuantity:12}
]:await fetchInventoryTaskItems(record.id);detailVisible.value=true}catch(e){Message.error(e instanceof Error?e.message:'盘点明细加载失败')}}
async function approve(record:InventoryTask){try{await approveInventoryTask(record.id,record.version,'盘点差异已复核');Message.success('盘点审核通过');await load()}catch(e){Message.error(e instanceof Error?e.message:'审核失败')}}
async function reject(record:InventoryTask){try{await rejectInventoryTask(record.id,record.version,'盘点数据异常，请重新核对');Message.success('盘点已驳回');await load()}catch(e){Message.error(e instanceof Error?e.message:'驳回失败')}}
async function closeTask(record:InventoryTask){try{await closeInventoryTask(record.id,record.version);Message.success('盘点任务已关账');await load()}catch(e){Message.error(e instanceof Error?e.message:'关账失败')}}
function difference(record:InventoryCountItem){return Number(record.countedQuantity||0)-Number(record.bookQuantity||0)}
function statusInfo(s:string){return({DRAFT:{label:'草稿',color:'gray'},IN_PROGRESS:{label:'盘点中',color:'blue'},PENDING_REVIEW:{label:'待审核',color:'orange'},APPROVED:{label:'已审核',color:'green'},CLOSED:{label:'已关账',color:'purple'}} as any)[s]||{label:s,color:'gray'}}
function formatTime(value:string){return value?value.replace('T',' ').slice(0,16):'—'}
</script>
<style scoped>.diff-up{color:#f53f3f;font-weight:600}.diff-down{color:#00b42a;font-weight:600}</style>
