<template>
  <div class="cost-page">
    <header class="cost-header"><div><h2>BOM 管理</h2><p>维护菜品配方、审核版本，并在发布时固化成本快照。</p></div><a-space><a-button @click="openFoodSync">同步菜品</a-button><a-button type="primary" :disabled="!storeId" @click="openCreate"><template #icon><IconPlus /></template>新建 BOM</a-button></a-space></header>
    <a-card class="cost-summary"><div v-for="item in summary" :key="item.label" class="summary-item"><span>{{item.label}}</span><strong>{{item.value}}</strong><small>{{item.note}}</small></div></a-card>
    <a-card class="cost-panel">
      <div class="panel-toolbar"><a-space><span>当前门店</span><a-select v-model="storeId" class="store-select" @change="load"><a-option v-for="store in stores" :key="store.id" :value="store.id">{{store.name}}</a-option></a-select></a-space><a-input-search v-model="keyword" allow-clear placeholder="搜索菜品或状态" class="toolbar-search"/></div>
      <a-table :columns="columns" :data="filtered" :loading="loading" row-key="id" :pagination="{pageSize:10}">
        <template #dish="{record}"><div><strong>{{dishName(record.dishId)}}</strong><div class="source-label">菜品 #{{record.dishId}}</div></div></template>
        <template #version="{record}">V{{record.currentVersion}}</template>
        <template #status="{record}"><a-tag :color="statusInfo(record.status).color">{{statusInfo(record.status).label}}</a-tag></template>
        <template #updatedTime="{record}">{{formatTime(record.updatedTime)}}</template>
        <template #actions="{record}"><a-space><a-link v-if="record.status==='DRAFT'||record.status==='REJECTED'" @click="submit(record)">提交审核</a-link><a-link v-if="record.status==='PENDING'" status="success" @click="publish(record)">发布</a-link><a-link v-if="record.status==='PENDING'" status="danger" @click="reject(record)">驳回</a-link><a-link @click="openDetail(record)">详情/编辑</a-link></a-space></template>
        <template #empty><a-empty :description="storeId?'当前门店暂无 BOM':'请先选择门店'"/></template>
      </a-table>
    </a-card>
    <a-modal v-model:visible="syncVisible" title="请选择菜品" width="860px" :footer="false" unmount-on-close>
      <a-form :model="syncQuery" layout="inline" class="sync-filter">
        <a-form-item required><a-select v-model="syncQuery.shopId" allow-search placeholder="请选择门店" class="sync-store-select" @change="searchSyncFoods"><a-option v-for="shop in syncSourceShops" :key="shop.relateid" :value="shop.relateid">{{shop.deptName}}</a-option></a-select></a-form-item>
        <a-form-item><a-input v-model="syncQuery.foodName" allow-clear placeholder="请输入菜品名称" class="sync-filter-input" @press-enter="searchSyncFoods"/></a-form-item>
        <a-form-item><a-input v-model="syncQuery.foodCode" allow-clear placeholder="请输入菜品编码" class="sync-filter-input" @press-enter="searchSyncFoods"/></a-form-item>
        <a-form-item><a-button type="primary" :loading="syncLoading" @click="searchSyncFoods">查询</a-button></a-form-item>
      </a-form>
      <a-space class="sync-actions"><a-button type="primary" :loading="syncSaving" @click="saveSyncSelection">保存菜品</a-button><a-button type="primary" :loading="syncSaving" @click="saveEverySyncFood">保存全部菜品</a-button><span class="sync-safe-tip">仅读取旧接口，保存到本地 SaaS</span></a-space>
      <a-table v-model:selected-keys="syncSelectedKeys" :columns="syncColumns" :data="syncRows" row-key="foodID" :loading="syncLoading" :row-selection="{type:'checkbox',showCheckedAll:true}" :pagination="false">
        <template #price="{record}"><span v-if="record.foodPrices?.length">{{formatSyncPrices(record)}}</span><span v-else>-</span></template>
        <template #empty><a-empty description="请选择接口门店并查询菜品"/></template>
      </a-table>
      <div class="sync-pagination"><span>共 {{syncTotal}} 条</span><a-pagination v-model:current="syncQuery.pageNum" v-model:page-size="syncQuery.pageSize" :total="syncTotal" show-total show-page-size :page-size-options="[10,20,50]" @change="loadSyncFoods" @page-size-change="changeSyncPageSize"/></div>
    </a-modal>
    <a-modal v-model:visible="visible" :title="editingId ? (editable ? 'BOM 详情与编辑' : 'BOM 详情') : '新建 BOM 草稿'" width="760px" :ok-loading="saving" :ok-button-props="{disabled:!editable}" @ok="save">
      <a-form :model="form" layout="vertical">
        <a-form-item label="菜品" required><a-select v-model="form.dishId" :disabled="!!editingId" allow-search placeholder="选择菜品"><a-option v-for="dish in dishes" :key="dish.id" :value="dish.id">{{dish.dishCode}} · {{dish.dishName}}</a-option></a-select></a-form-item>
        <a-form-item label="物料明细" required>
          <div class="bom-items"><div v-for="(item,index) in form.items" :key="index" class="bom-item-row"><a-select v-model="item.materialId" :disabled="!editable" allow-search placeholder="物料" class="material-select"><a-option v-for="material in materials" :key="material.id" :value="material.id">{{material.materialName}}</a-option></a-select><a-input-number v-model="item.quantity" :disabled="!editable" :min="0.000001" placeholder="用量" class="quantity-input"/><a-select v-model="item.unitId" :disabled="!editable" placeholder="单位" class="unit-select"><a-option v-for="unit in units" :key="unit.id" :value="unit.id">{{unit.unitName}}</a-option></a-select><a-button v-if="editable" status="danger" type="text" @click="form.items.splice(index,1)">移除</a-button></div><a-button v-if="editable" long @click="addItem"><template #icon><IconPlus/></template>添加物料</a-button></div>
        </a-form-item>
        <a-form-item label="备注"><a-textarea v-model="form.remark" :disabled="!editable"/></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script setup lang="ts">
import {computed,onMounted,reactive,ref} from 'vue';
import {Message,type TableColumnData} from '@arco-design/web-vue';
import {IconPlus} from '@arco-design/web-vue/es/icon';
import {createCostBom,fetchCostBomDetail,fetchCostBoms,fetchCostDishes,fetchCostMaterials,fetchCostUnits,fetchStores,fetchSyncFoodCandidates,fetchSyncFoodSourceShops,saveSelectedSyncFoods,saveAllSyncFoods,publishCostBom,rejectCostBom,submitCostBom,updateCostBom,type CostBom,type CostDish,type CostMaterial,type CostUnit,type SyncFoodCandidate,type SyncFoodSourceShop} from '@tql-store/api';
import type { StoreOption } from '@tql-store/shared';
import '../styles/cost-workspace.css';
const stores=ref<StoreOption[]>([]),storeId=ref<number>(),records=ref<CostBom[]>([]),dishes=ref<CostDish[]>([]),materials=ref<CostMaterial[]>([]),units=ref<CostUnit[]>([]),loading=ref(false),saving=ref(false),visible=ref(false),keyword=ref('');
const form=reactive({dishId:undefined as number|undefined,remark:'',items:[] as Array<{materialId?:number;unitId?:number;quantity?:number}>});
const syncVisible=ref(false),syncLoading=ref(false),syncSaving=ref(false),syncRows=ref<SyncFoodCandidate[]>([]),syncTotal=ref(0),syncSelectedKeys=ref<number[]>([]);
const syncSourceShops=ref<SyncFoodSourceShop[]>([]);
const syncQuery=reactive({shopId:undefined as number|undefined,foodName:'',foodCode:'',pageNum:1,pageSize:10});
const syncColumns:TableColumnData[]=[{title:'菜品编码',dataIndex:'foodCode',width:180},{title:'菜品名称',dataIndex:'foodName'},{title:'售价',slotName:'price',width:200}];
const editingId=ref<number>(),editingVersion=ref(0);
const editingStatus=ref<CostBom['status']>('DRAFT');
const editable=computed(()=>!editingId.value||editingStatus.value==='DRAFT'||editingStatus.value==='REJECTED');
const columns:TableColumnData[]=[{title:'菜品',slotName:'dish'},{title:'版本',slotName:'version',width:90},{title:'状态',slotName:'status',width:110},{title:'更新时间',slotName:'updatedTime',width:180},{title:'操作',slotName:'actions',width:230}];
const filtered=computed(()=>{const k=keyword.value.trim().toLowerCase();return k?records.value.filter(r=>`${dishName(r.dishId)}${r.status}`.toLowerCase().includes(k)):records.value});
const summary=computed(()=>[{label:'全部 BOM',value:records.value.length,note:'当前门店配方'},{label:'待审核',value:records.value.filter(i=>i.status==='PENDING').length,note:'等待成本人员处理'},{label:'已发布',value:records.value.filter(i=>i.status==='PUBLISHED').length,note:'已固化成本快照'}]);
const preview=import.meta.env.DEV&&new URLSearchParams(window.location.search).get('preview')==='1';
const previewFoods:SyncFoodCandidate[]=[
  {foodID:1380,shopID:1,foodCode:'01380',foodName:'炸鸡排',foodPrices:[{foodPrice:'22.00',unit:'份'}]},
  {foodID:1379,shopID:1,foodCode:'01379',foodName:'香辣鸡翅',foodPrices:[{foodPrice:'18.00',unit:'份'}]},
  {foodID:8001,shopID:1,foodCode:'E8001',foodName:'炒烧卖',foodPrices:[{foodPrice:'10.00',unit:'份'}]}
];
onMounted(async()=>{if(preview){stores.value=[{id:1,parentId:0,code:'STORE-001',name:'同庆楼 · 高新店'}];storeId.value=1;dishes.value=[{id:1,dishCode:'FOOD-001',dishName:'扬州炒饭',sourceSystem:'HUALALA',status:1},{id:2,dishCode:'FOOD-002',dishName:'红烧肉',sourceSystem:'HUALALA',status:1}];materials.value=[{id:1,materialCode:'MAT-001',materialName:'东北珍珠米',baseUnitId:1,sourceSystem:'KINGDEE',status:1}];units.value=[{id:1,unitCode:'KG',unitName:'千克',decimalScale:3,status:1}];records.value=[{id:1,storeId:1,dishId:1,status:'PUBLISHED',currentVersion:3,rowVersion:4,updatedTime:'2026-07-29T10:18:00'},{id:2,storeId:1,dishId:2,status:'PENDING',currentVersion:1,rowVersion:1,updatedTime:'2026-07-29T09:42:00'}];return}try{[stores.value,dishes.value,materials.value,units.value]=await Promise.all([fetchStores(),fetchCostDishes(),fetchCostMaterials(),fetchCostUnits()]);if(stores.value.length){storeId.value=stores.value[0].id;await load()}}catch(e){Message.error(e instanceof Error?e.message:'BOM基础数据加载失败')}});
async function load(){if(!storeId.value)return;loading.value=true;try{records.value=await fetchCostBoms(storeId.value)}catch(e){records.value=[];Message.error(e instanceof Error?e.message:'BOM列表加载失败')}finally{loading.value=false}}
async function openFoodSync(){
  syncVisible.value=true;
  if(preview){syncSourceShops.value=[{relateid:1,deptName:'同庆楼大仓万达店'}];syncQuery.shopId=1;return loadSyncFoods()}
  syncLoading.value=true;
  try{syncSourceShops.value=await fetchSyncFoodSourceShops()}catch(e){Message.error(e instanceof Error?e.message:'旧接口门店读取失败，请确认本地旧服务已启动')}finally{syncLoading.value=false}
}
function searchSyncFoods(){syncQuery.pageNum=1;loadSyncFoods()}
async function loadSyncFoods(){
  if(!syncQuery.shopId)return Message.warning('请先填写接口门店 ID');
  syncLoading.value=true;
  try{
    const result=preview?{rows:previewFoods,total:previewFoods.length}:await fetchSyncFoodCandidates({...syncQuery,shopId:syncQuery.shopId});
    syncRows.value=result.rows;syncTotal.value=result.total;syncSelectedKeys.value=result.rows.map(row=>row.foodID);
  }catch(e){syncRows.value=[];syncTotal.value=0;Message.error(e instanceof Error?e.message:'旧接口菜品读取失败，请确认本地旧服务已启动')}finally{syncLoading.value=false}
}
function changeSyncPageSize(size:number){syncQuery.pageSize=size;syncQuery.pageNum=1;loadSyncFoods()}
async function saveSyncSelection(){
  const rows=syncRows.value.filter(row=>syncSelectedKeys.value.includes(row.foodID));
  if(!rows.length)return Message.warning('请先选择菜品');
  syncSaving.value=true;
  try{const count=preview?rows.length:await saveSelectedSyncFoods(rows);Message.success(`已同步 ${count} 个菜品到本地 SaaS`)}catch(e){Message.error(e instanceof Error?e.message:'菜品同步失败')}finally{syncSaving.value=false}
}
async function saveEverySyncFood(){
  if(!syncQuery.shopId)return Message.warning('请先填写接口门店 ID');
  if(!syncTotal.value)return Message.warning('当前没有可同步菜品');
  syncSaving.value=true;
  try{const count=preview?syncTotal.value:await saveAllSyncFoods({shopId:syncQuery.shopId,foodCode:syncQuery.foodCode||undefined,foodName:syncQuery.foodName||undefined});Message.success(`已同步 ${count} 个菜品到本地 SaaS`)}catch(e){Message.error(e instanceof Error?e.message:'全部菜品同步失败')}finally{syncSaving.value=false}
}
function openCreate(){editingId.value=undefined;editingVersion.value=0;editingStatus.value='DRAFT';Object.assign(form,{dishId:undefined,remark:'',items:[]});addItem();visible.value=true}function addItem(){form.items.push({})}
async function openDetail(record:CostBom){try{const detail=preview?{id:record.id,dishId:record.dishId,rowVersion:record.rowVersion,remark:'门店标准配方',items:[{materialId:1,unitId:1,quantity:.15}]}:await fetchCostBomDetail(record.id);editingId.value=detail.id;editingVersion.value=detail.rowVersion;editingStatus.value=record.status;form.dishId=detail.dishId;form.remark=detail.remark||'';form.items=detail.items.map(i=>({materialId:i.materialId,unitId:i.unitId,quantity:i.quantity}));visible.value=true}catch(e){Message.error(e instanceof Error?e.message:'BOM详情加载失败')}}
async function save(){if(!storeId.value||!form.dishId||!form.items.length||form.items.some(i=>!i.materialId||!i.unitId||!i.quantity))return Message.warning('请完整填写菜品和物料明细');saving.value=true;try{const items=form.items.map((i,n)=>({materialId:i.materialId!,unitId:i.unitId!,quantity:i.quantity!,sortOrder:n}));if(editingId.value)await updateCostBom(editingId.value,{expectedVersion:editingVersion.value,remark:form.remark,items});else await createCostBom({storeId:storeId.value,dishId:form.dishId,remark:form.remark,items});visible.value=false;Message.success(editingId.value?'BOM 已更新':'BOM 草稿创建成功');await load()}catch(e){Message.error(e instanceof Error?e.message:'保存失败')}finally{saving.value=false}}
async function submit(record:CostBom){try{await submitCostBom(record.id,record.rowVersion);Message.success('已提交审核');await load()}catch(e){Message.error(e instanceof Error?e.message:'提交失败')}}async function publish(record:CostBom){try{await publishCostBom(record.id,record.rowVersion);Message.success('BOM已发布并生成成本快照');await load()}catch(e){Message.error(e instanceof Error?e.message:'发布失败')}}
async function reject(record:CostBom){try{await rejectCostBom(record.id,record.rowVersion,'审核驳回，请修改配方');Message.success('已驳回 BOM');await load()}catch(e){Message.error(e instanceof Error?e.message:'驳回失败')}}
function formatSyncPrices(record:SyncFoodCandidate){return record.foodPrices?.map(item=>`${item.foodPrice} / ${item.unit}`).join('、')||'-'}
function dishName(id:number){return dishes.value.find(d=>d.id===id)?.dishName||`菜品 ${id}`}function formatTime(v:string){return v?v.replace('T',' ').slice(0,16):'—'}function statusInfo(s:string){const statusMap:Record<string,{label:string;color:string}>={DRAFT:{label:'草稿',color:'gray'},PENDING:{label:'待审核',color:'orange'},PUBLISHED:{label:'已发布',color:'green'},REJECTED:{label:'已驳回',color:'red'},DISABLED:{label:'已停用',color:'gray'}};return statusMap[s]||{label:s,color:'gray'}}
</script>
<style scoped>.store-select{width:240px}.toolbar-search{width:260px}.sync-store-select{width:210px}.sync-filter-input{width:180px}.bom-items{width:100%;padding:var(--tql-space-3);border:1px solid var(--tql-border);border-radius:var(--tql-radius-control);background:var(--tql-bg-subtle)}.bom-item-row{display:flex;gap:var(--tql-space-2);margin-bottom:var(--tql-space-2)}.material-select{flex:1}.quantity-input{width:150px}.unit-select{width:130px}.sync-filter{margin-bottom:var(--tql-space-1)}.sync-actions{margin:var(--tql-space-2) 0 var(--tql-space-4)}.sync-safe-tip{margin-left:var(--tql-space-2);color:var(--tql-text-tertiary);font-size:12px}.sync-pagination{display:flex;align-items:center;justify-content:space-between;margin-top:var(--tql-space-4);color:var(--tql-text-tertiary)}</style>
