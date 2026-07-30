<template>
  <div class="container">
    <a-card class="tql-search-card">
      <div class="tql-search-layout">
        <a-form class="tql-search-form" :model="query" label-align="left">
          <div class="tql-search-fields">
            <a-form-item label="关键词"><a-input v-model="query.keyword" allow-clear placeholder="图标名称或编码" /></a-form-item>
            <a-form-item label="分类"><a-input v-model="query.category" allow-clear placeholder="全部分类" /></a-form-item>
            <a-form-item label="状态"><a-select v-model="query.status" allow-clear placeholder="全部状态"><a-option :value="1">启用</a-option><a-option :value="0">停用</a-option></a-select></a-form-item>
          </div>
        </a-form>
        <a-divider class="tql-search-divider" direction="vertical" />
        <div class="tql-search-actions"><a-space><a-button type="primary" @click="load"><template #icon><IconSearch /></template>查询</a-button><a-button @click="reset"><template #icon><IconRefresh /></template>重置</a-button></a-space></div>
      </div>
    </a-card>
    <a-card class="list-card tql-list-card">
      <div class="toolbar"><a-space><a-button v-if="can('platform:system:icon:create')" type="primary" @click="openUpload"><template #icon><IconUpload /></template>上传图标</a-button><span class="total">共 {{ records.length }} 个</span></a-space><a-button type="text" @click="load"><template #icon><IconRefresh /></template>刷新</a-button></div>
      <a-table row-key="id" :columns="columns" :data="records" :loading="loading" :pagination="false">
        <template #preview="{record}"><MenuIcon :code="record.code" :svg="record.svgContent" /></template>
        <template #source="{record}"><a-tag :color="record.sourceType==='SYSTEM'?'arcoblue':'green'">{{ record.sourceType==='SYSTEM'?'系统内置':'自定义' }}</a-tag></template>
        <template #status="{record}"><a-badge :status="record.status===1?'success':'normal'" :text="record.status===1?'启用':'停用'" /></template>
        <template #actions="{record}"><a-space><a-link v-if="can('platform:system:icon:update')" @click="openEdit(record)">编辑</a-link><a-link v-if="can('platform:system:icon:update')" @click="toggle(record)">{{record.status===1?'停用':'启用'}}</a-link><a-popconfirm v-if="record.sourceType==='CUSTOM'&&can('platform:system:icon:delete')" content="确定删除该图标吗？" @ok="remove(record)"><a-link status="danger">删除</a-link></a-popconfirm></a-space></template>
      </a-table>
    </a-card>
    <a-modal v-model:visible="modalVisible" :title="editing?'编辑图标':'上传图标'" :ok-loading="saving" @ok="submit">
      <a-form :model="form" layout="vertical">
        <a-form-item label="图标名称" required><a-input v-model="form.name" maxlength="64" /></a-form-item>
        <a-form-item v-if="!editing" label="图标编码" required><a-input v-model="form.code" placeholder="例如 custom-order" /></a-form-item>
        <a-form-item label="分类" required><a-input v-model="form.category" /></a-form-item>
        <a-form-item label="排序"><a-input-number v-model="form.order" :min="0" class="full" /></a-form-item>
        <a-form-item v-if="!editing" label="SVG文件" required><input type="file" accept=".svg,image/svg+xml" @change="pickFile" /><div class="tip">仅支持包含 viewBox 的安全 SVG，最大 50KB</div></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { Message, type TableColumnData } from '@arco-design/web-vue';
import { IconRefresh, IconSearch, IconUpload } from '@arco-design/web-vue/es/icon';
import { deleteIcon, fetchIcons, updateIcon, updateIconStatus, uploadIcon } from '@tql-store/api';
import { usePermission } from '@tql-store/auth';
import type { IconItem } from '@tql-store/shared';
import MenuIcon from '../components/MenuIcon.vue';
const {can}=usePermission();
const query=reactive<{keyword:string;category:string;status?:number}>({keyword:'',category:''});
const records=ref<IconItem[]>([]),loading=ref(false),modalVisible=ref(false),saving=ref(false),editing=ref<IconItem>(),file=ref<File>();
const form=reactive({name:'',code:'',category:'导航',order:0});
const columns:TableColumnData[]=[{title:'预览',slotName:'preview',width:72},{title:'名称',dataIndex:'name'},{title:'编码',dataIndex:'code'},{title:'分类',dataIndex:'category'},{title:'来源',slotName:'source',width:100},{title:'使用数量',dataIndex:'usageCount',width:100},{title:'排序',dataIndex:'order',width:80},{title:'状态',slotName:'status',width:90},{title:'操作',slotName:'actions',width:180}];
async function load(){loading.value=true;try{records.value=await fetchIcons({keyword:query.keyword||undefined,category:query.category||undefined,status:query.status});}catch(e){Message.error(e instanceof Error?e.message:'加载失败');}finally{loading.value=false;}}
function reset(){query.keyword='';query.category='';query.status=undefined;load();}
function openUpload(){editing.value=undefined;Object.assign(form,{name:'',code:'',category:'导航',order:0});file.value=undefined;modalVisible.value=true;}
function openEdit(row:IconItem){editing.value=row;Object.assign(form,{name:row.name,code:row.code,category:row.category,order:row.order});modalVisible.value=true;}
function pickFile(e:Event){file.value=(e.target as HTMLInputElement).files?.[0];}
async function submit(){if(!form.name.trim()||!form.category.trim()){Message.warning('请填写完整信息');return;}saving.value=true;try{if(editing.value)await updateIcon(editing.value.id,form);else{if(!file.value||!form.code.trim()){Message.warning('请选择SVG并填写编码');return;}const data=new FormData();data.append('name',form.name);data.append('code',form.code);data.append('category',form.category);data.append('order',String(form.order));data.append('file',file.value);await uploadIcon(data);}Message.success('保存成功');modalVisible.value=false;await load();}catch(e){Message.error(e instanceof Error?e.message:'保存失败');}finally{saving.value=false;}}
async function toggle(row:IconItem){try{await updateIconStatus(row.id,row.status===1?0:1);await load();}catch(e){Message.error(e instanceof Error?e.message:'操作失败');}}
async function remove(row:IconItem){try{await deleteIcon(row.id);Message.success('删除成功');await load();}catch(e){Message.error(e instanceof Error?e.message:'删除失败');}}
onMounted(load);
</script>
<style scoped>
.container{width:100%;padding:var(--tql-page-padding)}.list-card{border-color:var(--tql-border);box-shadow:none}.toolbar{display:flex;justify-content:space-between;margin-bottom:16px}.total,.tip{color:var(--tql-text-tertiary);font-size:12px}.full{width:100%}:deep(.arco-table-th){background:var(--tql-bg-subtle)}
</style>
