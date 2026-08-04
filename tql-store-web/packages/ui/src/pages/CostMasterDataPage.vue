<template>
  <div class="cost-page">
    <header class="cost-header">
      <div><h2>成本主数据</h2><p>统一维护 BOM 与盘点使用的单位、物料和菜品。</p></div>
      <a-button type="primary" @click="openCreate"><template #icon><IconPlus /></template>新增{{ activeLabel }}</a-button>
    </header>
    <a-card class="cost-summary">
      <div v-for="item in summary" :key="item.label" class="summary-item">
        <span>{{ item.label }}</span><strong>{{ item.value }}</strong><small>{{ item.note }}</small>
      </div>
    </a-card>
    <a-card class="cost-panel">
      <div class="panel-toolbar">
        <a-tabs v-model:active-key="active" @change="load"><a-tab-pane key="materials" title="物料"/><a-tab-pane key="units" title="计量单位"/><a-tab-pane key="dishes" title="菜品"/></a-tabs>
        <a-input-search v-model="keyword" allow-clear placeholder="搜索编码或名称" class="toolbar-search" />
      </div>
      <a-table :columns="columns" :data="filteredRecords" :loading="loading" row-key="id" :pagination="{ pageSize: 10 }">
        <template #status="{ record }"><a-tag :color="record.status === 1 ? 'green' : 'gray'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag></template>
        <template #sourceSystem="{ record }"><span class="source-label">{{ record.sourceSystem || 'SAAS' }}</span></template>
        <template #empty><a-empty description="暂无主数据，点击右上角开始创建" /></template>
      </a-table>
    </a-card>
    <a-modal v-model:visible="visible" :title="`新增${activeLabel}`" :ok-loading="saving" @ok="save">
      <a-form :model="form" layout="vertical">
        <template v-if="active === 'units'">
          <a-form-item label="单位编码" required><a-input v-model="form.code" placeholder="例如 KG" /></a-form-item>
          <a-form-item label="单位名称" required><a-input v-model="form.name" placeholder="例如 千克" /></a-form-item>
          <a-form-item label="数量精度"><a-input-number v-model="form.scale" :min="0" :max="10" class="full-width-control" /></a-form-item>
        </template>
        <template v-else>
          <a-form-item :label="`${activeLabel}编码`" required><a-input v-model="form.code" /></a-form-item>
          <a-form-item :label="`${activeLabel}名称`" required><a-input v-model="form.name" /></a-form-item>
          <a-form-item label="外部系统编码"><a-input v-model="form.externalCode" placeholder="金蝶/哗啦啦编码" /></a-form-item>
          <a-form-item v-if="active === 'materials'" label="基本单位" required>
            <a-select v-model="form.baseUnitId" placeholder="请选择"><a-option v-for="unit in units" :key="unit.id" :value="unit.id">{{ unit.unitName }}</a-option></a-select>
          </a-form-item>
        </template>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Message, type TableColumnData } from '@arco-design/web-vue';
import { IconPlus } from '@arco-design/web-vue/es/icon';
import {
  createCostDish, createCostMaterial, createCostUnit, fetchCostDishes,
  fetchCostMaterials, fetchCostUnits, type CostDish, type CostMaterial, type CostUnit
} from '@tql-store/api';
import '../styles/cost-workspace.css';

type Tab = 'materials' | 'units' | 'dishes';
const active = ref<Tab>('materials');
const loading = ref(false);
const saving = ref(false);
const visible = ref(false);
const keyword = ref('');
const units = ref<CostUnit[]>([]);
const materials = ref<CostMaterial[]>([]);
const dishes = ref<CostDish[]>([]);
type MasterRecord = CostUnit | CostMaterial | CostDish;
const form = reactive({ code: '', name: '', externalCode: '', baseUnitId: undefined as number | undefined, scale: 6 });
const activeLabel = computed(() => ({ materials: '物料', units: '计量单位', dishes: '菜品' }[active.value]));
const records = computed<MasterRecord[]>(() => active.value === 'materials' ? materials.value : active.value === 'units' ? units.value : dishes.value);
const filteredRecords = computed(() => {
  const key = keyword.value.trim().toLowerCase();
  if (!key) return records.value;
  return records.value.filter(item => Object.values(item).some(value => String(value ?? '').toLowerCase().includes(key)));
});
const summary = computed(() => [
  { label: '物料', value: materials.value.length, note: '统一成本核算口径' },
  { label: '计量单位', value: units.value.length, note: '支持精确单位换算' },
  { label: '菜品', value: dishes.value.length, note: '关联门店 BOM' }
]);
const columns = computed<TableColumnData[]>(() => active.value === 'units'
  ? [{ title: '单位编码', dataIndex: 'unitCode' }, { title: '单位名称', dataIndex: 'unitName' }, { title: '数量精度', dataIndex: 'decimalScale' }, { title: '状态', slotName: 'status', width: 100 }]
  : [{ title: `${activeLabel.value}编码`, dataIndex: active.value === 'materials' ? 'materialCode' : 'dishCode' },
      { title: `${activeLabel.value}名称`, dataIndex: active.value === 'materials' ? 'materialName' : 'dishName' },
      { title: '外部编码', dataIndex: active.value === 'materials' ? 'externalMaterialCode' : 'externalDishCode' },
      { title: '来源', slotName: 'sourceSystem', width: 120 }, { title: '状态', slotName: 'status', width: 100 }]);

const preview = import.meta.env.DEV && new URLSearchParams(window.location.search).get('preview') === '1';
onMounted(() => {
  if (preview) {
    units.value = [{ id: 1, unitCode: 'KG', unitName: '千克', decimalScale: 3, status: 1 }, { id: 2, unitCode: 'BOX', unitName: '箱', decimalScale: 2, status: 1 }];
    materials.value = [
      { id: 1, materialCode: 'MAT-10001', materialName: '东北珍珠米', specification: '25kg/袋', baseUnitId: 1, externalMaterialCode: 'KD-10001', sourceSystem: 'KINGDEE', status: 1 },
      { id: 2, materialCode: 'MAT-10002', materialName: '一级大豆油', specification: '10L/桶', baseUnitId: 1, externalMaterialCode: 'KD-10002', sourceSystem: 'KINGDEE', status: 1 }
    ];
    dishes.value = [{ id: 1, dishCode: 'FOOD-001', dishName: '扬州炒饭', externalDishCode: 'HLL-001', sourceSystem: 'HUALALA', status: 1 }];
    return;
  }
  loadAll();
});
async function loadAll() {
  loading.value = true;
  try { [units.value, materials.value, dishes.value] = await Promise.all([fetchCostUnits(), fetchCostMaterials(), fetchCostDishes()]); }
  catch (error) { Message.error(error instanceof Error ? error.message : '主数据加载失败'); }
  finally { loading.value = false; }
}
function load() { keyword.value = ''; }
function openCreate() {
  Object.assign(form, { code: '', name: '', externalCode: '', baseUnitId: undefined, scale: 6 });
  visible.value = true;
}
async function save() {
  if (!form.code.trim() || !form.name.trim()) return Message.warning('请填写编码和名称');
  saving.value = true;
  try {
    if (active.value === 'units') await createCostUnit({ unitCode: form.code, unitName: form.name, decimalScale: form.scale });
    else if (active.value === 'materials') {
      if (!form.baseUnitId) throw new Error('请选择基本单位');
      await createCostMaterial({ materialCode: form.code, materialName: form.name, externalMaterialCode: form.externalCode || undefined, baseUnitId: form.baseUnitId, sourceSystem: 'SAAS' });
    } else await createCostDish({ dishCode: form.code, dishName: form.name, externalDishCode: form.externalCode || undefined, sourceSystem: 'SAAS' });
    visible.value = false; Message.success(`${activeLabel.value}创建成功`); await loadAll();
  } catch (error) { Message.error(error instanceof Error ? error.message : '保存失败'); }
  finally { saving.value = false; }
}
</script>

<style scoped>
.toolbar-search { width: 260px; }
.full-width-control { width: 100%; }
</style>
