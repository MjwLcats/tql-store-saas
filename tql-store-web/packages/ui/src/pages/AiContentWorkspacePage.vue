<template>
  <div class="video-center">
    <template v-if="!createVisible">
    <section v-if="activeModule === 'plans'" class="module-content">
      <a-card class="search-panel" :bordered="false">
        <a-form :model="filters" class="plan-search-form" layout="inline">
          <a-form-item class="plan-search-item" label="计划类型">
            <a-select v-model="filters.type" class="plan-query-control" placeholder="全部类型" allow-clear>
              <a-option value="半原创">半原创</a-option>
              <a-option value="原创">原创</a-option>
            </a-select>
          </a-form-item>
          <a-form-item class="plan-search-item" label="计划名称"><a-input v-model="filters.keyword" class="plan-query-control" placeholder="请输入计划名称" allow-clear /></a-form-item>
          <a-form-item class="plan-search-item" label="下发状态">
            <a-select v-model="filters.status" class="plan-query-control" placeholder="全部状态" allow-clear>
              <a-option value="草稿">草稿</a-option>
              <a-option value="进行中">进行中</a-option>
              <a-option value="已暂停">已暂停</a-option>
              <a-option value="已终止">已终止</a-option>
              <a-option value="已完结">已完结</a-option>
            </a-select>
          </a-form-item>
          <a-form-item class="plan-search-item" label="创建人"><a-input v-model="filters.owner" class="plan-query-control" placeholder="工号/姓名" allow-clear /></a-form-item>
          <a-form-item class="plan-search-item plan-date-item" label="发布日期"><a-range-picker v-model="filters.dateRange" /></a-form-item>
        </a-form>
        <div class="search-actions">
          <a-button @click="resetFilters"><template #icon><IconRefresh /></template>重置</a-button>
          <a-button v-if="can(P.planQuery)" type="primary" @click="applyFilters"><template #icon><IconSearch /></template>查询</a-button>
        </div>
      </a-card>

      <a-card class="data-panel" :bordered="false">
        <div class="panel-toolbar">
          <div>
            <strong>发布计划</strong>
            <span>共 {{ visiblePlans.length }} 条</span>
          </div>
          <a-space>
            <!-- 列表导出入口暂时隐藏，保留导出逻辑以便后续恢复。
            <a-button v-if="can(P.planExportDelivery)" @click="exportDetails"><template #icon><IconDownload /></template>导出人员</a-button>
            <a-button v-if="can(P.planExportMaterial)" @click="exportMaterials"><template #icon><IconDownload /></template>导出素材收集</a-button>
            -->
            <a-button v-if="can(P.planCreate)" type="primary" @click="openCreate()"><template #icon><IconPlus /></template>新建</a-button>
          </a-space>
        </div>
        <a-table
          row-key="id"
          :columns="planColumns"
          :data="visiblePlans"
          :loading="loading"
          :pagination="{ pageSize: 8, showTotal: true }"
          :scroll="{ x: 1450 }"
        >
          <template #type="{ record }">
            <a-tag :color="record.type === '半原创' ? 'arcoblue' : 'purple'">{{ record.type }}</a-tag>
          </template>
          <template #status="{ record }">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template #period="{ record }">
            <div>{{ record.startDate }}</div><small>至 {{ record.endDate }}</small>
          </template>
          <template #personProgress="{ record }">
            <div class="progress-cell">
              <a-progress :percent="record.personProgress / 100" size="small" :show-text="false" />
              <span>{{ record.completed }}/{{ record.accounts }}</span>
            </div>
          </template>
          <template #videoProgress="{ record }">
            <div class="progress-cell">
              <a-progress :percent="record.videoProgress / 100" size="small" :show-text="false" />
              <span>{{ record.completedVideos }}/{{ record.totalVideos }}</span>
            </div>
          </template>
          <template #actions="{ record }">
            <a-space size="mini">
              <a-link v-if="can(P.planUpdate) && ['草稿', '待开始'].includes(record.status)" @click="openEdit(record)">编辑</a-link>
              <a-link v-if="can(P.planCancel) && record.status === '进行中'" @click="pausePlan(record)">暂停</a-link>
              <a-link v-if="can(P.planCancel) && record.status === '已暂停'" @click="resumePlan(record)">恢复</a-link>
              <a-link v-if="can(P.planView)" @click="viewPlan(record)">查看</a-link>
              <a-link v-if="can(P.planDelivery)" @click="viewDelivery(record)">人员</a-link>
              <a-link v-if="can(P.planCancel) && ['进行中', '已暂停'].includes(record.status)" status="danger" @click="terminatePlan(record)">终止</a-link>
              <a-link v-if="can(P.planCancel) && ['草稿', '待开始'].includes(record.status)" status="danger" @click="deletePlan(record)">删除</a-link>
            </a-space>
          </template>
          <template #empty>
            <div class="empty-state">
              <div class="empty-illustration"><IconVideoCamera /></div>
              <strong>暂无发布计划</strong>
              <span>创建第一个计划，向员工下发短视频拍摄任务</span>
              <a-button v-if="can(P.planCreate)" type="primary" @click="openCreate()">新建发布计划</a-button>
            </div>
          </template>
        </a-table>
      </a-card>
    </section>

    <section v-else-if="activeModule === 'calendar'" class="module-content">
      <a-card class="calendar-panel" :bordered="false">
        <div class="calendar-toolbar">
          <div class="month-switch">
            <a-button shape="circle" aria-label="上一个月" @click="shiftMonth(-1)"><IconLeft /></a-button>
            <strong>{{ calendarYear }}年{{ calendarMonth }}月</strong>
            <a-button shape="circle" aria-label="下一个月" @click="shiftMonth(1)"><IconRight /></a-button>
            <a-button @click="goToday">今天</a-button>
          </div>
          <div class="legend">
            <span><i class="legend-create"></i>可创建</span>
            <span><i class="legend-draft"></i>草稿</span>
            <span><i class="legend-active"></i>进行中</span>
            <span><i class="legend-completed"></i>已完成</span>
            <span><i class="legend-expired"></i>已过期</span>
          </div>
        </div>
        <div class="weekday-row"><span v-for="day in weekDays" :key="day">{{ day }}</span></div>
        <div class="calendar-grid">
          <article
            v-for="cell in calendarCells"
            :key="cell.key"
            class="calendar-day"
            :class="{ muted: !cell.current, today: cell.today }"
          >
            <div class="day-head"><strong>{{ cell.day }}</strong><span v-if="cell.tasks.length">{{ cell.tasks.length }}个任务</span></div>
            <div
              v-if="cell.tasks.length"
              class="day-tasks"
              :class="{ scrollable: cell.tasks.length > 2 }"
              :tabindex="cell.tasks.length > 2 ? 0 : undefined"
              :aria-label="cell.tasks.length > 2 ? `${cell.date}共有${cell.tasks.length}条任务，可上下滑动查看更多` : undefined"
            >
              <button v-for="task in cell.tasks" :key="task.id" type="button" :class="`task-${task.tone}`" :disabled="!can(P.calendarView)" @click="viewPlan(task)">
                <span class="task-meta">
                  <span>{{ task.type }}</span>
                  <a-tag size="small" :color="calendarStatusColor(task.tone)">{{ task.status }}</a-tag>
                </span>
                <strong>{{ task.name }}</strong>
                <span class="calendar-task-progress">
                  <a-progress :percent="task.progress / 100" size="small" :show-text="false" />
                  <span v-if="task.accounts">{{ task.completed }}/{{ task.accounts }} · {{ task.progress }}%</span>
                  <span v-else>未下发</span>
                </span>
              </button>
            </div>
            <button v-else-if="cell.current && can(P.calendarCreate)" class="create-day" type="button" @click="openCreate(cell.date)">+ 创建计划</button>
          </article>
        </div>
      </a-card>
    </section>

    <section v-else-if="activeModule === 'analytics'" class="module-content">
      <div class="platform-tabs">
        <button v-for="platform in platforms" :key="platform.name" :class="{ active: activePlatform === platform.name }" @click="activePlatform = platform.name">
          <span :class="`platform-mark ${platform.className}`">{{ platform.short }}</span>{{ platform.name }}
        </button>
      </div>
      <a-card class="search-panel compact" :bordered="false">
        <a-form :model="analyticsFilters" layout="inline">
          <a-form-item label="任务/视频ID"><a-input v-model="analyticsFilters.id" placeholder="请输入任务ID或平台视频ID" /></a-form-item>
          <a-form-item label="发布日期"><a-range-picker v-model="analyticsFilters.dateRange" /></a-form-item>
        </a-form>
        <div class="search-actions"><a-button @click="resetAnalytics">重置</a-button><a-button v-if="can(P.analyticsQuery)" type="primary" @click="loadVideoReports()">查询</a-button></div>
      </a-card>
      <div class="metric-grid">
        <article v-for="metric in analyticsMetrics" :key="metric.label" :class="metric.tone">
          <span>{{ metric.label }}</span><strong>{{ metric.value }}</strong>
          <small>{{ metric.hint }}</small>
        </article>
      </div>
      <a-card class="trend-panel" :bordered="false">
        <div class="panel-title"><div><strong>视频发布效果明细</strong><span>数据由各平台开放接口同步，未同步时不生成模拟数据</span></div></div>
        <a-table row-key="id" :columns="videoReportColumns" :data="filteredVideoReports" :loading="videoReportsLoading" :scroll="{ x: 1900 }" :pagination="{ pageSize: 10 }">
          <template #video="{ record }"><div class="video-report-title"><strong>{{ record.videoTitle }}</strong><a v-if="record.videoUrl" :href="record.videoUrl" target="_blank">查看视频</a></div></template>
          <template #amount="{ record }">¥{{ Number(record.transactionAmount).toFixed(2) }}</template>
          <template #syncStatus="{ record }"><a-tag :color="record.syncStatus === 'SUCCESS' ? 'green' : record.syncStatus === 'FAILED' ? 'red' : 'orange'">{{ reportSyncLabel[record.syncStatus] || record.syncStatus }}</a-tag></template>
        </a-table>
      </a-card>
    </section>

    <section v-else-if="activeModule === 'accounts'" class="module-content">
      <a-card class="search-panel" :bordered="false">
        <a-form :model="accountFilters" layout="inline">
          <a-form-item label="账号名称/ID"><a-input v-model="accountFilters.keyword" placeholder="请输入" allow-clear /></a-form-item>
          <a-form-item label="账号类型"><a-select v-model="accountFilters.type" placeholder="请选择" allow-clear><a-option value="蓝V">蓝V</a-option><a-option value="职人">职人</a-option><a-option value="个人">个人</a-option></a-select></a-form-item>
          <a-form-item label="账号状态"><a-select v-model="accountFilters.status" placeholder="请选择" allow-clear><a-option value="正常">正常</a-option><a-option value="待校验">待校验</a-option><a-option value="校验失败">校验失败</a-option><a-option value="授权失效">授权失效</a-option><a-option value="停用">停用</a-option></a-select></a-form-item>
          <a-form-item label="员工工号/姓名"><a-input v-model="accountFilters.employee" placeholder="请输入" allow-clear /></a-form-item>
        </a-form>
        <div class="search-actions"><a-button @click="resetAccounts">重置</a-button><a-button v-if="can(P.accountQuery)" type="primary" @click="loadAccounts()">查询</a-button></div>
      </a-card>
      <a-card class="data-panel" :bordered="false">
        <div class="platform-tabs inner">
          <button v-for="platform in platforms" :key="platform.name" :class="{ active: accountPlatform === platform.name }" @click="accountPlatform = platform.name">
            <span :class="`platform-mark ${platform.className}`">{{ platform.short }}</span>{{ platform.name }}
          </button>
          <div class="platform-actions">
            <a-button v-if="can(P.accountCreate)" type="primary" @click="openAccountEditor()"><template #icon><IconPlus /></template>新增账号</a-button>
            <a-button v-if="can(P.accountDelete) && selectedAccountIds.length" status="danger" @click="removeAccounts(selectedAccountIds)">批量删除</a-button>
            <a-button v-if="can(P.accountImport)" @click="openImport"><template #icon><IconUpload /></template>导入账号</a-button>
            <a-button v-if="can(P.accountExport)" @click="exportAccounts"><template #icon><IconDownload /></template>下载账号</a-button>
          </div>
        </div>
        <a-table row-key="id" :columns="accountColumns" :data="filteredAccounts" :loading="accountsLoading" :row-selection="{ type: 'checkbox', showCheckedAll: true }" v-model:selected-keys="selectedAccountIds" :pagination="{ pageSize: 8 }">
          <template #platform="{ record }"><span class="account-platform"><i :class="record.platformClass">{{ record.platformShort }}</i>{{ record.platform }}</span></template>
          <template #status="{ record }"><span class="status-dot" :class="record.status === '正常' ? 'status-active' : 'status-draft'">{{ record.status }}</span></template>
          <template #actions="{ record }"><a-space size="mini"><a-link v-if="can(P.accountDetailView)" @click="openAccountEditor(record, true)">查看</a-link><a-link v-if="can(P.accountUpdate)" @click="openAccountEditor(record)">编辑</a-link><a-link v-if="can(P.accountDelete)" status="danger" @click="removeAccounts([record.id])">删除</a-link><a-link @click="viewPlatformHomepage(record)">查看平台主页</a-link></a-space></template>
        </a-table>
      </a-card>
      </section>

    <section v-else class="module-content material-library">
      <div class="material-workbench">
        <aside class="material-type-rail">
          <a-input-search
            v-model="folderKeyword"
            class="folder-search-input"
            placeholder="搜索文件夹"
            allow-clear
            @search="reloadMaterialFolders"
            @press-enter="reloadMaterialFolders"
            @clear="reloadMaterialFolders"
          />
          <a-tree
            v-if="materialTreeData.length"
            class="material-folder-tree"
            :data="materialTreeData"
            :selected-keys="materialTreeSelectedKeys"
            :default-expanded-keys="materialTreeData.map(n => n.key)"
            block-node
            @select="onMaterialTreeSelect"
          />
          <a-empty v-else description="暂无文件夹" />
          <a-button v-if="can(P.bgmCreate)" class="folder-create-btn" type="text" long @click="openMaterialFolderEditor()">
            <template #icon><IconPlus /></template>新建文件夹
          </a-button>
        </aside>

        <main class="material-content">
          <a-card v-if="currentMaterialFolderId !== 0" class="search-panel folder-search-panel" :bordered="false">
            <div class="folder-breadcrumb">
              <a-button type="text" size="small" @click="leaveMaterialFolder"><IconLeft />返回文件夹</a-button>
              <span class="breadcrumb-divider">/</span>
              <span class="breadcrumb-current">{{ currentMaterialFolderName }}</span>
            </div>
            <a-form v-if="activeMaterialTab === 'AUDIO'" :model="bgmFilters" layout="inline">
              <a-form-item label="素材名称"><a-input v-model="bgmFilters.keyword" placeholder="名称或文件名" allow-clear /></a-form-item>
              <a-form-item label="适用视频"><a-select v-model="bgmFilters.videoType" placeholder="全部类型" allow-clear><a-option v-for="item in bgmVideoTypes" :key="item" :value="item">{{ item }}</a-option></a-select></a-form-item>
              <a-form-item label="情绪氛围"><a-select v-model="bgmFilters.mood" placeholder="全部氛围" allow-clear><a-option v-for="item in bgmMoods" :key="item" :value="item">{{ item }}</a-option></a-select></a-form-item>
              <a-form-item label="状态"><a-select v-model="bgmFilters.enabled" placeholder="全部状态" allow-clear><a-option :value="true">启用</a-option><a-option :value="false">停用</a-option></a-select></a-form-item>
            </a-form>
            <a-form v-else :model="materialFilters" layout="inline">
              <a-form-item label="素材名称"><a-input v-model="materialFilters.keyword" placeholder="名称、文件名或标签" allow-clear /></a-form-item>
              <a-form-item label="素材分类"><a-select v-model="materialFilters.category" placeholder="全部分类" allow-clear><a-option v-for="item in activeMaterialMeta.categories" :key="item" :value="item">{{ item }}</a-option></a-select></a-form-item>
              <a-form-item label="状态"><a-select v-model="materialFilters.enabled" placeholder="全部状态" allow-clear><a-option :value="true">启用</a-option><a-option :value="false">停用</a-option></a-select></a-form-item>
            </a-form>
            <div class="search-actions"><a-button @click="resetActiveMaterialFilters">重置</a-button><a-button type="primary" @click="loadActiveMaterials">查询</a-button></div>
          </a-card>

          <a-card class="data-panel" :bordered="false">
            <div class="panel-toolbar">
              <div><strong>{{ activeMaterialMeta.label }}管理</strong><span>共 {{ activeMaterialCount }} 条</span></div>
              <a-button v-if="can(P.bgmCreate) && currentMaterialFolderId !== 0" type="primary" @click="openActiveMaterialEditor">
                <template #icon><IconPlus /></template>上传{{ activeMaterialMeta.label }}
              </a-button>
            </div>
            <div v-if="currentMaterialFolderId === 0" class="material-folder-grid" v-loading="folderLoading">
              <div class="material-folder-card" @click="enterMaterialFolder(-1)">
                <div class="folder-icon"><IconFolder /></div>
                <div class="folder-info">
                  <strong>未分类</strong>
                  <span>{{ uncategorizedMaterialCount }} 个素材</span>
                </div>
              </div>
              <div v-for="folder in materialFolders" :key="folder.id" class="material-folder-card" :class="{ disabled: !folder.enabled }" @click="enterMaterialFolder(folder.id)">
                <div class="folder-icon"><IconFolder /></div>
                <div class="folder-info">
                  <strong>{{ folder.folderName }}</strong>
                  <span>{{ folder.itemCount }} 个素材 · {{ formatDate(folder.updateTime) }}</span>
                </div>
                <a-tag size="small" class="folder-status" :color="folder.enabled ? 'green' : 'gray'">{{ folder.enabled ? '启用' : '停用' }}</a-tag>
                <a-dropdown trigger="click" @select="(value) => handleFolderAction(folder, String(value))">
                  <a-button type="text" class="folder-more-btn" aria-label="文件夹操作" @click.stop>
                    <template #icon><IconMoreVertical /></template>
                  </a-button>
                  <template #content>
                    <a-doption value="toggle" :disabled="!can(P.bgmUpdate)">{{ folder.enabled ? '停用' : '启用' }}</a-doption>
                    <a-doption value="rename" :disabled="!can(P.bgmUpdate)">重命名</a-doption>
                    <a-doption value="delete" :disabled="!can(P.bgmDelete) || folder.itemCount > 0">删除</a-doption>
                  </template>
                </a-dropdown>
              </div>
            </div>
            <a-table v-else-if="activeMaterialTab === 'AUDIO'" row-key="id" :columns="bgmColumns" :data="activeBgms" :loading="bgmLoading" :scroll="{ x: 1500 }" :pagination="{ pageSize: 10 }">
              <template #audio="{ record }"><div class="bgm-name"><strong>{{ record.bgmName }}</strong><audio :src="record.fileUrl" controls preload="none" /></div></template>
              <template #classification="{ record }"><a-space wrap><a-tag color="arcoblue">{{ record.videoType }}</a-tag><a-tag color="purple">{{ record.mood }}</a-tag><a-tag>{{ record.energyLevel }}</a-tag></a-space></template>
              <template #duration="{ record }">{{ formatBgmDuration(record.durationSeconds) }}</template>
              <template #copyright="{ record }"><div class="bgm-copyright"><strong>{{ record.copyrightStatus }}</strong><span>{{ record.copyrightNote || '未填写范围说明' }}</span></div></template>
              <template #enabled="{ record }"><a-tag :color="record.enabled ? 'green' : 'gray'">{{ record.enabled ? '启用' : '停用' }}</a-tag></template>
              <template #actions="{ record }"><a-space size="mini"><a-link @click="openMoveMaterial(record)">移动</a-link><a-link v-if="can(P.bgmUpdate)" @click="openBgmEditor(record)">编辑</a-link><a-link v-if="can(P.bgmDelete)" status="danger" @click="removeBgm(record)">删除</a-link></a-space></template>
            </a-table>
            <a-table v-else row-key="id" :columns="materialColumns" :data="materials" :loading="materialLoading" :scroll="{ x: 1280 }" :pagination="{ pageSize: 10 }">
              <template #preview="{ record }">
                <div class="material-preview-cell">
                  <video v-if="record.materialType === 'VIDEO'" :src="record.fileUrl" preload="metadata" controls />
                  <img v-else :src="record.fileUrl" :alt="record.materialName" />
                  <div><strong>{{ record.materialName }}</strong><span>{{ record.originalFileName }}</span></div>
                </div>
              </template>
              <template #classification="{ record }"><a-space wrap><a-tag color="arcoblue">{{ record.category }}</a-tag><a-tag v-for="tag in splitMaterialTags(record.tags)" :key="tag">{{ tag }}</a-tag></a-space></template>
              <template #fileInfo="{ record }"><div class="material-file-info"><strong>{{ formatFileSize(record.fileSize) }}</strong><span>{{ materialExtension(record.originalFileName) }}</span></div></template>
              <template #copyright="{ record }"><div class="bgm-copyright"><strong>{{ record.copyrightStatus }}</strong><span>{{ record.copyrightNote || '未填写范围说明' }}</span></div></template>
              <template #enabled="{ record }"><a-tag :color="record.enabled ? 'green' : 'gray'">{{ record.enabled ? '启用' : '停用' }}</a-tag></template>
              <template #actions="{ record }"><a-space size="mini"><a-link :href="record.fileUrl" target="_blank">预览</a-link><a-link @click="openMoveMaterial(record)">移动</a-link><a-link v-if="can(P.bgmUpdate)" @click="openMaterialEditor(record)">编辑</a-link><a-link v-if="can(P.bgmDelete)" status="danger" @click="removeMaterial(record)">删除</a-link></a-space></template>
            </a-table>
          </a-card>
        </main>
      </div>
    </section>
    </template>

    <section v-else class="inline-wizard-page">
      <header class="inline-wizard-header">
        <a-button class="wizard-back" type="text" shape="circle" aria-label="返回发布计划" @click="leaveCreate">
          <template #icon><IconLeft /></template>
        </a-button>
        <strong>{{ editingActivityId ? '编辑发布计划' : '创建发布计划' }}</strong>
      </header>
      <div class="wizard-shell" @wheel="forwardWizardWheel">
        <div class="wizard-progress">
          <a-steps :current="wizardStep" label-placement="vertical">
            <a-step title="计划设置" description="设置计划名称与创作模式" />
            <a-step title="投放配置" description="配置时间、平台和执行账号" />
            <a-step title="创作配置" description="完善内容、分镜和素材参考" />
            <a-step title="检查下发" description="预览配置并创建员工任务" />
          </a-steps>
        </div>

        <main class="wizard-content">
          <div v-if="wizardStep === 1" class="wizard-card">
            <a-form :model="planForm" layout="vertical">
              <a-grid :cols="1" :col-gap="24">
                <a-grid-item>
                  <a-form-item label="计划名称" required>
                    <a-input v-model="planForm.name" size="large" placeholder="例如：夏日冰饮重点商品推广" :max-length="30" show-word-limit />
                  </a-form-item>
                </a-grid-item>
                <!-- 发布方式当前固定为员工任务，暂不在创建计划页面展示。
                <a-grid-item>
                  <a-form-item label="发布方式" required>
                    <a-select v-model="planForm.deliveryMode" size="large"><a-option value="员工任务">员工任务</a-option></a-select>
                  </a-form-item>
                </a-grid-item>
                -->
              </a-grid>
              <a-form-item label="下发模式" required>
                <div class="mode-options">
                  <button type="button" :class="{ selected: planForm.type === '半原创' }" @click="planForm.type = '半原创'">
                    <IconRobot /><strong>半原创</strong><span>提供分镜样例和台词，员工按照模板完成拍摄</span>
                  </button>
                  <button type="button" :class="{ selected: planForm.type === '原创' }" @click="planForm.type = '原创'">
                    <IconVideoCamera /><strong>原创</strong><span>提供整体拍摄要求，员工自主完成内容创作</span>
                  </button>
                </div>
              </a-form-item>
            </a-form>
          </div>

          <div v-else-if="wizardStep === 2" class="wizard-card">
            <a-form :model="planForm" layout="vertical">
              <a-grid :cols="2" :col-gap="24">
                <a-grid-item>
                  <a-form-item class="release-date-item" label="发布日期" required :extra="editingActivityId ? undefined : '为确保流程正常进行，发布日期必须大于当前时间3天'">
                    <a-range-picker
                      v-model="planForm.dateRange"
                      size="large"
                      class="full-width"
                      :disabled-date="disabledReleaseDate"
                      @change="validateReleaseDateSelection"
                    />
                  </a-form-item>
                </a-grid-item>
                <a-grid-item>
                  <a-form-item label="发布平台" required>
                    <a-checkbox-group v-model="planForm.platforms" class="platform-checks">
                      <a-checkbox
                        v-for="platform in supportedPublishPlatforms"
                        :key="platform"
                        :value="platform"
                        :disabled="planForm.platforms.length === 1 && planForm.platforms.includes(platform)"
                      >{{ platform }}</a-checkbox>
                    </a-checkbox-group>
                  </a-form-item>
                </a-grid-item>
              </a-grid>
              <a-form-item label="视频条数" required>
                <div class="platform-quota-list">
                  <div v-for="platform in planForm.platforms" :key="platform" class="platform-quota-row">
                    <strong>{{ platform }}</strong>
                    <label>
                      <span>需要发布</span>
                      <a-input-number v-model="planForm.platformQuotas[platform].total" :min="1" :precision="0" hide-button />
                      <span>条</span>
                    </label>
                    <label>
                      <span>每天可发送</span>
                      <a-input-number v-model="planForm.platformQuotas[platform].daily" :min="1" :precision="0" hide-button />
                      <span>条</span>
                    </label>
                  </div>
                </div>
              </a-form-item>
              <a-form-item label="任务开始时间" required extra="可选择发布时间；勾选“发布即开始”后，任务下发成功即进入开始状态并通知员工">
                <div class="task-start-setting">
                  <a-date-picker
                    v-model="planForm.taskStartTime"
                    class="task-start-picker"
                    size="large"
                    show-time
                    format="YYYY-MM-DD HH:mm:ss"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    placeholder="请选择任务开始时间"
                    :disabled="planForm.immediateStart"
                    :disabled-date="disabledTaskStartDate"
                    :disabled-time="disabledTaskStartTime"
                    @change="validateTaskStartTime"
                  />
                  <a-checkbox v-model="planForm.immediateStart" @change="handleImmediateStartChange">发布即开始</a-checkbox>
                </div>
              </a-form-item>
              <a-form-item label="发布账户" required>
                <div class="account-selector">
                  <a-radio-group v-model="planForm.accountMode"><a-radio value="账号">通过账号选择</a-radio><a-radio v-if="can(P.planImportEmployee)" value="人员">导入人员</a-radio></a-radio-group>
                  <a-button v-if="can(P.planSelectEmployee) && planForm.accountMode === '账号'" @click="openPlanAccountSelector">选择账号</a-button>
                  <a-button v-if="can(P.planImportEmployee) && planForm.accountMode === '人员'" @click="openPersonnelImport"><template #icon><IconUpload /></template>导入人员</a-button>
                  <span class="selected-count">
                    已选择 <strong>{{ planForm.accountMode === '账号' ? selectedPlanAccountIds.length : planForm.employeeCount }}</strong> {{ planForm.accountMode === '账号' ? '个账号' : '名员工' }}
                    <a-link v-if="planForm.accountMode === '人员' && planForm.employeeCount" @click="selectedPersonnelVisible = true">查看</a-link>
                  </span>
                </div>
              </a-form-item>
            </a-form>
          </div>

          <div v-else-if="wizardStep === 3" class="wizard-card">
            <a-form :model="planForm" layout="vertical" class="storyboard-content-form">
              <div class="description-heading">
                <strong><span>*</span>视频内容描述</strong>
                <a-button v-if="can(P.planAiScript)" type="outline" @click="openDescriptionGenerator"><template #icon><IconRobot /></template>AI生成视频内容描述</a-button>
              </div>
              <a-form-item>
                <a-textarea v-model="planForm.description" placeholder="请填写专业、可商用的视频主题、核心卖点、目标受众和表达方式" :max-length="1000" show-word-limit :auto-size="{ minRows: 4, maxRows: 7 }" />
              </a-form-item>
              <a-form-item label="话题">
                <div class="topic-field">
                  <div class="topic-editor">
                    <a-input
                      v-model="topicInput"
                      size="large"
                      placeholder="请输入话题名称，如：端午"
                      :max-length="30"
                      @press-enter="addTopic"
                    />
                    <a-button type="primary" size="large" :disabled="!topicInput.trim()" @click="addTopic">添加</a-button>
                  </div>
                  <div v-if="topicList.length" class="topic-list">
                    <a-tag v-for="topic in topicList" :key="topic" closable @close="removeTopic(topic)">#{{ topic }}#</a-tag>
                  </div>
                  <div class="topic-tip">输入话题后点击添加，将自动生成“#话题#”标准格式，最多添加 10 个</div>
                </div>
              </a-form-item>
            </a-form>
            <template v-if="planForm.type === '半原创'">
              <div class="config-stage-tabs" role="tablist" aria-label="分镜配置模块">
                <button type="button" role="tab" :aria-selected="activeConfigTab === 'basic'" :class="{ active: activeConfigTab === 'basic' }" @click="activeConfigTab = 'basic'">分镜基础设置</button>
                <button type="button" role="tab" :aria-selected="activeConfigTab === 'content'" :class="{ active: activeConfigTab === 'content' }" @click="activeConfigTab = 'content'">分镜内容</button>
                <button type="button" role="tab" :aria-selected="activeConfigTab === 'material'" :class="{ active: activeConfigTab === 'material' }" @click="activeConfigTab = 'material'">视频素材参考</button>
              </div>
              <div class="storyboard-reference-layout">
                <section v-show="activeConfigTab === 'basic'" class="storyboard-config-panel">
                  <div class="basic-setting-grid">
                    <div class="storyboard-setting-block">
                      <div class="setting-label"><strong><i>*</i> 分镜数量</strong><span>最多 8 个</span></div>
                      <a-input-number class="storyboard-count-input" v-model="planForm.storyboardCount" :min="1" :max="8" @change="syncStoryboards" />
                    </div>
                    <div class="storyboard-setting-block">
                      <div class="setting-label"><strong><i>*</i> 画面方向</strong><span>所有分镜统一使用</span></div>
                      <a-radio-group v-model="planForm.sampleAspect" type="button" class="aspect-options" @change="handleStoryboardAspectChange">
                        <a-radio value="portrait">竖版 9:16</a-radio>
                        <a-radio value="landscape">横版 16:9</a-radio>
                      </a-radio-group>
                    </div>
                  </div>
                </section>
                <section v-show="activeConfigTab === 'material'" class="storyboard-config-panel material-reference-panel">
                  <div class="material-reference-fields">
                    <div class="storyboard-setting-block cover-setting-block">
                      <div class="setting-label"><strong>视频封面模板示例</strong><span>选填</span></div>
                      <a-upload :auto-upload="false" :show-file-list="false" :limit="1" accept="image/jpeg,image/png,image/webp" @change="uploadCoverExample">
                        <template #upload-button>
                          <div class="cover-example-upload">
                            <img v-if="planForm.coverExampleUrl" :src="planForm.coverExampleUrl" alt="视频封面模板示例" />
                            <IconUpload v-else />
                            <div><strong>{{ planForm.coverUploading ? '正在上传...' : planForm.coverExampleUrl ? '重新上传封面示例' : '上传封面示例' }}</strong><span>{{ planForm.coverExampleName || `JPG / PNG / WEBP，${sampleAspectText(planForm.sampleAspect)}` }}</span></div>
                          </div>
                        </template>
                      </a-upload>
                    </div>
                    <div class="storyboard-setting-block bgm-reference-block">
                      <div class="setting-label"><strong>BGM</strong><span>选填 · 仅显示已启用</span></div>
                      <a-select v-model="planForm.bgmIds" multiple allow-clear :max-tag-count="2" :loading="planBgmLoading" placeholder="可选择多个 BGM">
                        <a-option v-for="bgm in planBgmOptions" :key="bgm.id" :value="bgm.id" :label="`${bgm.bgmName} · ${bgm.videoType} / ${bgm.mood}`" />
                      </a-select>
                    </div>
                    <div class="storyboard-setting-block voice-reference-block">
                      <div class="setting-label"><strong>配音</strong><span>选填 · 同步员工 App</span></div>
                      <a-select v-model="planForm.voiceStyle" allow-clear placeholder="请选择建议配音">
                        <a-option v-for="voice in voiceStyleOptions" :key="voice" :value="voice">{{ voice }}</a-option>
                      </a-select>
                    </div>
                  </div>
                  <div v-if="selectedPlanBgms.length" class="selected-bgm-list">
                    <div v-for="bgm in selectedPlanBgms" :key="bgm.id" class="selected-bgm-item">
                      <div><strong>{{ bgm.bgmName }}</strong><span>{{ bgm.videoType }} · {{ bgm.mood }} · {{ bgm.energyLevel }}</span></div>
                      <audio class="plan-bgm-audio" :src="bgm.fileUrl" controls preload="none" />
                    </div>
                  </div>
                </section>
              </div>
              <div v-show="activeConfigTab === 'content'" class="storyboard-content-panel">
                <div class="storyboard-editor-actions">
                  <a-button type="outline" :loading="generating" @click="generateScripts"><template #icon><IconRobot /></template>AI生成分镜台词</a-button>
                </div>
                <a-tabs v-model:active-key="activeStoryboard" type="card-gutter">
                <a-tab-pane v-for="(storyboard, index) in storyboards" :key="index" :title="`分镜 ${index + 1}`">
                  <a-form :model="storyboard" layout="vertical">
                    <a-grid class="storyboard-editor-grid" :cols="25" :col-gap="28">
                      <a-grid-item :span="13" class="storyboard-copy-column">
                        <a-form-item label="拍摄要求" required><a-textarea v-model="storyboard.requirement" placeholder="例如：在光线明亮的场景拍摄，产品保持在画面中央" :auto-size="{ minRows: 5 }" /></a-form-item>
                        <a-form-item label="分镜台词" required>
                          <div class="script-field-content">
                            <a-textarea v-model="storyboard.script" placeholder="点击“AI生成分镜台词”一键生成，也可手动修改" :max-length="300" show-word-limit :auto-size="{ minRows: 5 }" />
                            <div class="duration-tip">预计话术时长 {{ estimateDuration(storyboard.script) }} 秒</div>
                          </div>
                        </a-form-item>
                        <a-form-item label="分镜时长要求" required>
                          <div class="storyboard-duration-range">
                            <a-input-number v-model="storyboard.minDuration" :min="1" :max="storyboard.maxDuration" :precision="0" hide-button />
                            <span>秒</span>
                            <i>～</i>
                            <a-input-number v-model="storyboard.maxDuration" :min="storyboard.minDuration" :max="300" :precision="0" hide-button />
                            <span>秒</span>
                          </div>
                        </a-form-item>
                      </a-grid-item>
                      <a-grid-item :span="12" class="storyboard-media-column">
                        <a-form-item label="样例视频" required>
                          <a-upload
                            :auto-upload="false"
                            :show-file-list="false"
                            :limit="1"
                            accept="video/mp4"
                            @change="(files, fileItem) => uploadStoryboardSample(index, files, fileItem)"
                          >
                            <template #upload-button>
                              <div class="video-upload large" :class="planForm.sampleAspect === 'landscape' ? 'landscape' : 'portrait'">
                                <IconUpload />
                                <strong>{{ storyboard.uploading ? '正在上传...' : storyboard.sampleVideoUrl ? '重新上传样例视频' : '上传样例视频' }}</strong>
                                <span v-if="storyboard.sampleVideoName">{{ storyboard.sampleVideoName }}<br />{{ sampleAspectText(planForm.sampleAspect) }}，员工端按比例展示</span>
                                <span v-else>支持 MP4，最大 200MB<br />请上传{{ sampleAspectText(planForm.sampleAspect) }}视频</span>
                              </div>
                            </template>
                          </a-upload>
                        </a-form-item>
                      </a-grid-item>
                    </a-grid>
                  </a-form>
                </a-tab-pane>
                </a-tabs>
              </div>
            </template>
            <template v-else>
              <a-alert type="info">原创任务不配置分镜，员工将根据以下要求自主完成短视频创作。</a-alert>
              <div class="original-requirement-heading">
                <strong><span>*</span>原创拍摄要求</strong>
                <a-button type="outline" :loading="generatingShootingRequirement" @click="generateShootingRequirement"><template #icon><IconRobot /></template>AI生成拍摄要求</a-button>
              </div>
              <a-form-item class="original-requirement">
                <a-textarea v-model="planForm.originalRequirement" placeholder="请说明时长、场景、出镜、商品展示、口播等要求" :auto-size="{ minRows: 10 }" />
              </a-form-item>
            </template>
          </div>

          <div v-else class="wizard-card confirm-card">
            <div class="confirm-summary">
              <div><span>计划名称</span><strong>{{ planForm.name }}</strong></div>
              <div><span>任务类型</span><strong>{{ planForm.type }}</strong></div>
              <div><span>发布日期</span><strong>{{ planForm.dateRange.join(' 至 ') || '未设置' }}</strong></div>
              <div><span>任务开始时间</span><strong>{{ planForm.immediateStart ? '发布即开始' : (planForm.taskStartTime || '未设置') }}</strong></div>
              <div><span>发布账号</span><strong>{{ selectedPlanAccountIds.length }} 个</strong></div>
              <div><span>发布平台与数量</span><strong>{{ platformQuotaSummary }}</strong></div>
              <div><span>{{ planForm.type === '半原创' ? '分镜数量' : '创作方式' }}</span><strong>{{ planForm.type === '半原创' ? `${planForm.storyboardCount} 个` : '员工原创' }}</strong></div>
              <div v-if="planForm.type === '半原创'"><span>画面规格</span><strong>{{ sampleAspectText(planForm.sampleAspect) }}</strong></div>
              <div v-if="planForm.type === '半原创'"><span>分镜内容</span><strong>{{ storyboards.filter((item) => item.requirement && item.script && item.sampleVideoUrl && item.minDuration > 0 && item.maxDuration >= item.minDuration).length }}/{{ planForm.storyboardCount }} 个已完善</strong></div>
              <div v-if="planForm.type === '半原创'"><span>素材参考</span><strong>封面 {{ planForm.coverExampleUrl ? '已上传' : '未上传' }} · BGM {{ selectedPlanBgms.length }} 个 · 配音 {{ planForm.voiceStyle || '未选择' }}</strong></div>
            </div>
            <a-alert type="warning">提交后将按选定账号创建员工任务。建议确认人员范围、发布日期和分镜内容无误后再下发。</a-alert>
            <div class="confirm-copy"><strong>视频内容描述</strong><p>{{ planForm.description }}</p></div>
            <div v-if="topicList.length" class="confirm-copy"><strong>发布话题</strong><p>{{ formattedTopics }}</p></div>
          </div>
        </main>
        <div class="wizard-footer">
          <div class="wizard-footer-actions">
            <a-button v-if="!editingActivityId && can(P.planSave)" type="text" :loading="submitting" @click="saveDraft">保存草稿</a-button>
            <a-space>
              <a-button v-if="wizardStep > 1" @click="wizardStep--">上一步</a-button>
              <a-button v-if="wizardStep < 4" type="primary" @click="nextWizardStep">{{ wizardStep === 3 ? '预览并确认' : '下一步' }}</a-button>
              <a-button v-else-if="editingActivityId ? can(P.planUpdate) : can(P.planPublish)" type="primary" :loading="submitting" @click="submitPlan">{{ editingActivityId ? '保存修改' : '确认并下发' }}</a-button>
            </a-space>
          </div>
        </div>
      </div>
    </section>

    <a-modal
      v-model:visible="descriptionAiVisible"
      title="AI生成视频内容描述"
      width="680px"
      :ok-loading="generatingDescription"
      :ok-button-props="{ disabled: !descriptionPrompt.trim() }"
      :cancel-button-props="{ disabled: generatingDescription }"
      :closable="!generatingDescription"
      :mask-closable="!generatingDescription"
      :on-before-ok="generateVideoDescription"
      ok-text="生成并填入"
    >
      <a-alert type="info">请用自然语言说明想拍什么、面向谁、突出哪些卖点以及希望呈现的风格，AI会整理为专业、可商用的视频内容描述。</a-alert>
      <a-textarea
        v-model="descriptionPrompt"
        class="description-ai-input"
        placeholder="例如：想拍一条介绍门店夏季新品的视频，面向年轻上班族，突出清爽口感和午后休闲场景，语气轻松自然。"
        :max-length="2000"
        show-word-limit
        :auto-size="{ minRows: 7, maxRows: 12 }"
      />
    </a-modal>

    <a-drawer v-model:visible="detailVisible" title="发布计划详情" :width="760">
      <div v-if="selectedPlan" class="detail-page">
        <div class="detail-hero">
          <div><a-tag :color="selectedPlan.type === '半原创' ? 'arcoblue' : 'purple'">{{ selectedPlan.type }}</a-tag><h2>{{ selectedPlan.name }}</h2><p>计划ID：{{ selectedPlan.id }} · 创建人：{{ selectedPlan.owner }}</p></div>
          <span class="status-dot" :class="statusClass(selectedPlan.status)">{{ selectedPlan.status }}</span>
        </div>
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="任务时间">{{ selectedPlan.startDate }} 至 {{ selectedPlan.endDate }}</a-descriptions-item>
          <a-descriptions-item label="发布账号">{{ selectedPlan.accounts }} 个</a-descriptions-item>
          <a-descriptions-item label="执行进度(人员)">{{ selectedPlan.completed }}/{{ selectedPlan.accounts }}（{{ selectedPlan.personProgress }}%）</a-descriptions-item>
          <a-descriptions-item label="执行进度(视频)">{{ selectedPlan.completedVideos }}/{{ selectedPlan.totalVideos }}（{{ selectedPlan.videoProgress }}%）</a-descriptions-item>
          <a-descriptions-item label="发布平台">抖音、快手、小红书、视频号</a-descriptions-item>
        </a-descriptions>
        <h3>执行进度(人员)</h3><a-progress :percent="selectedPlan.personProgress / 100" />
        <h3>执行进度(视频)</h3><a-progress :percent="selectedPlan.videoProgress / 100" />
        <h3>任务说明</h3><p class="detail-copy">围绕本期重点商品完成短视频拍摄，需按照分镜样例依次上传素材，合成后发布至指定平台并回传作品链接。</p>
      </div>
    </a-drawer>

    <a-modal v-model:visible="deliveryVisible" title="人员" width="900px" :footer="false">
      <div class="delivery-summary"><span>计划ID：{{ selectedPlan?.id }}</span><span>计划名称：{{ selectedPlan?.name }}</span><a-button v-if="can(P.planExportDelivery)" @click="exportDetails"><IconDownload /> 导出Excel</a-button></div>
      <a-table row-key="taskId" :columns="deliveryColumns" :data="deliveryRows" :loading="deliveryLoading" :pagination="{ pageSize: 5 }">
        <template #period="{ record }">
          <div class="delivery-period">
            <span>{{ record.periodStart }}</span>
            <span>至 {{ record.periodEnd }}</span>
          </div>
        </template>
        <template #status="{ record }"><a-tag :color="record.statusColor">{{ record.status }}</a-tag></template>
      </a-table>
    </a-modal>

    <a-modal v-model:visible="personnelImportVisible" title="导入人员" width="1080px" :footer="false">
      <a-alert type="info">请使用系统模板填写姓名、手机号。点击“校验人员”后，系统将与用户表逐条核对姓名和手机号，只有校验通过的人员可以加入下发名单。</a-alert>
      <div class="personnel-import-toolbar">
        <a-button v-if="can(P.planImportEmployee)" @click="downloadPersonnelTemplate"><template #icon><IconDownload /></template>下载Excel模板</a-button>
        <a-upload :auto-upload="false" accept=".xlsx" :limit="1" @change="handlePersonnelFile">
          <template #upload-button><a-button><template #icon><IconUpload /></template>选择Excel文件</a-button></template>
        </a-upload>
        <span class="personnel-file-name">{{ personnelImportFile?.name || '尚未选择文件' }}</span>
        <a-button v-if="can(P.planValidateEmployee)" type="primary" :loading="personnelValidating" :disabled="!personnelImportFile" @click="validatePersonnelFile">校验人员</a-button>
      </div>
      <a-table
        row-key="rowNumber"
        :columns="personnelImportColumns"
        :data="personnelImportRows"
        :loading="personnelValidating"
        :pagination="{ pageSize: 8 }"
        :scroll="{ x: 980 }"
      >
        <template #validation="{ record }">
          <a-tag :color="record.status === 'VALID' ? 'green' : record.status === 'MISMATCH' ? 'orange' : 'red'">{{ record.message }}</a-tag>
        </template>
      </a-table>
      <div class="personnel-import-footer">
        <span>共 {{ personnelImportRows.length }} 条，通过 {{ validPersonnelRows.length }} 条</span>
        <a-space><a-button @click="personnelImportVisible = false">取消</a-button><a-button type="primary" :disabled="!validPersonnelRows.length" @click="applyImportedPersonnel">添加校验通过人员</a-button></a-space>
      </div>
    </a-modal>

    <a-modal v-model:visible="selectedPersonnelVisible" title="已选员工明细" width="920px" :footer="false">
      <a-alert class="selected-personnel-alert" type="info">
        当前计划共选择 {{ selectedPersonnelRows.length }} 名员工，组织选择与导入人员统一按以下字段展示。
      </a-alert>
      <a-table
        row-key="id"
        :columns="selectedPersonnelColumns"
        :data="selectedPersonnelRows"
        :pagination="{ pageSize: 8, showTotal: true }"
        :scroll="{ x: 760 }"
      />
    </a-modal>

    <a-modal v-model:visible="planAccountVisible" title="选择发布账号" width="1080px" :ok-loading="accountsLoading" @ok="confirmPlanAccounts">
      <a-input-search v-model="planAccountKeyword" class="employee-search" size="large" allow-clear placeholder="搜索账号名称、平台账号ID或归属员工" />
      <a-table
        v-model:selected-keys="selectedPlanAccountIds"
        row-key="id"
        :columns="planAccountColumns"
        :data="selectablePlanAccounts"
        :loading="accountsLoading"
        :row-selection="{ type: 'checkbox', showCheckedAll: true }"
        :pagination="{ pageSize: 8, showTotal: true }"
        :scroll="{ x: 900 }"
      >
        <template #status="{ record }"><span class="status-dot" :class="record.status === '正常' ? 'status-active' : 'status-draft'">{{ record.status }}</span></template>
      </a-table>
    </a-modal>

    <a-modal v-model:visible="employeeVisible" title="选择组织/人员" width="960px" :ok-loading="employeesLoading" @ok="confirmEmployees">
      <a-input-search v-model="employeeKeyword" class="employee-search" size="large" allow-clear placeholder="搜索员工姓名、工号或组织" />
      <div class="org-selector">
        <div class="org-tree">
          <div class="selector-heading"><strong>组织与可下发员工</strong><span>共 {{ employees.length }} 人</span></div>
          <a-spin :loading="employeesLoading" class="employee-tree-loading">
            <OrganizationCollapseNode
              v-if="employeeVisible && employeeTreeData.length"
              v-model:selected-keys="selectedEmployeeIds"
              :nodes="employeeTreeData"
            />
            <a-empty v-else-if="!employeesLoading" description="暂无匹配的组织或可下发员工" />
          </a-spin>
        </div>
        <div class="selected-org">
          <div class="selector-heading"><strong>已选员工</strong><span>{{ selectedEmployeeIds.length }} 人</span></div>
          <div v-if="selectedEmployees.length" class="selected-employee-list">
            <div v-for="employee in selectedEmployees" :key="employee.id" class="selected-employee">
              <span>{{ employee.displayName }}</span>
              <div class="selected-employee-meta">
                <small>工号：{{ employee.employeeNumber || employee.username || '未设置' }}</small>
                <small>手机：{{ employee.phone || '未设置' }}</small>
              </div>
              <button type="button" aria-label="移除员工" @click="removeEmployee(employee.id)">×</button>
            </div>
          </div>
          <a-empty v-else description="暂未选择员工" />
          <p class="selection-tip">任务只下发给已启用且可登录的员工</p>
        </div>
      </div>
    </a-modal>

    <a-modal v-model:visible="accountEditorVisible" :title="accountReadonly ? '查看账号' : accountEditingId ? '编辑账号' : '新增账号'" width="620px" :ok-button-props="{ disabled: accountReadonly }" @ok="saveAccount">
      <a-form ref="accountFormRef" :model="accountForm" :rules="accountRules" layout="vertical" :disabled="accountReadonly">
        <div class="account-form-grid">
          <a-form-item field="platform" label="发布平台" required><a-select v-model="accountForm.platform"><a-option v-for="item in platforms.slice(1)" :key="item.name" :value="item.name">{{ item.name }}</a-option></a-select></a-form-item>
          <a-form-item field="accountType" label="账号类型" required><a-select v-model="accountForm.accountType"><a-option value="蓝V">蓝V</a-option><a-option value="职人">职人</a-option><a-option value="个人">个人</a-option></a-select></a-form-item>
          <a-form-item field="accountName" label="账号名称" required><a-input v-model.trim="accountForm.accountName" :max-length="128" show-word-limit /></a-form-item>
          <a-form-item field="platformAccountId" label="平台账号ID" required><a-input v-model.trim="accountForm.platformAccountId" :max-length="128" /></a-form-item>
          <a-form-item field="platformHomepageUrl" label="平台主页地址"><a-input v-model.trim="accountForm.platformHomepageUrl" :max-length="500" placeholder="请输入以 http:// 或 https:// 开头的主页地址" /></a-form-item>
          <a-form-item field="employeeId" label="归属员工" required><a-select v-model="accountForm.employeeId" allow-search :loading="employeesLoading" placeholder="请选择已启用且可登录的员工" @change="syncAccountOrganization"><a-option v-for="employee in employees" :key="employee.id" :value="employee.id">{{ employee.displayName }}（{{ employee.employeeNumber || employee.username }}）</a-option></a-select></a-form-item>
          <a-alert class="account-status-tip" type="info">账号保存后状态为“待校验”。接入对应开放平台后，系统将根据授权或接口校验结果自动更新状态。</a-alert>
        </div>
      </a-form>
    </a-modal>

    <a-drawer v-model:visible="bgmEditorVisible" :title="bgmEditingId ? '编辑音频素材' : '上传音频素材'" :width="720" :ok-loading="bgmSaving" @ok="saveBgm">
      <a-alert type="info">按视频用途、情绪氛围和节奏强度分类，便于运营人员检索，也便于后续由 AI 自动推荐配乐。</a-alert>
      <a-form ref="bgmFormRef" :model="bgmForm" layout="vertical" class="bgm-form">
        <a-form-item label="音频文件" required>
          <a-upload :auto-upload="false" :limit="1" accept=".mp3,.wav,.m4a,audio/*" @change="handleBgmFile"><template #upload-button><a-button :loading="bgmUploading"><IconUpload /> {{ bgmForm.originalFileName || '选择音频文件' }}</a-button></template></a-upload>
        </a-form-item>
        <a-form-item field="bgmName" label="素材名称" required :rules="[{ required: true, message: '请输入素材名称' }]"><a-input v-model="bgmForm.bgmName" :max-length="128" /></a-form-item>
        <a-grid :cols="2" :col-gap="16">
          <a-grid-item><a-form-item field="videoType" label="适用视频类型" required :rules="[{ required: true, message: '请选择视频类型' }]"><a-select v-model="bgmForm.videoType"><a-option v-for="item in bgmVideoTypes" :key="item" :value="item">{{ item }}</a-option></a-select></a-form-item></a-grid-item>
          <a-grid-item><a-form-item field="mood" label="情绪氛围" required :rules="[{ required: true, message: '请选择情绪氛围' }]"><a-select v-model="bgmForm.mood"><a-option v-for="item in bgmMoods" :key="item" :value="item">{{ item }}</a-option></a-select></a-form-item></a-grid-item>
          <a-grid-item><a-form-item label="节奏强度" required><a-select v-model="bgmForm.energyLevel"><a-option v-for="item in bgmEnergyLevels" :key="item" :value="item">{{ item }}</a-option></a-select></a-form-item></a-grid-item>
          <a-grid-item><a-form-item label="人声类型" required><a-select v-model="bgmForm.vocalType"><a-option v-for="item in bgmVocalTypes" :key="item" :value="item">{{ item }}</a-option></a-select></a-form-item></a-grid-item>
          <a-grid-item><a-form-item label="BPM"><a-input-number v-model="bgmForm.bpm" :min="20" :max="300" placeholder="可选" /></a-form-item></a-grid-item>
          <a-grid-item><a-form-item label="时长（秒）"><a-input-number v-model="bgmForm.durationSeconds" :min="1" :max="7200" placeholder="可选" /></a-form-item></a-grid-item>
          <a-grid-item><a-form-item label="版权状态" required><a-select v-model="bgmForm.copyrightStatus"><a-option v-for="item in bgmCopyrightStatuses" :key="item" :value="item">{{ item }}</a-option></a-select></a-form-item></a-grid-item>
          <a-grid-item><a-form-item label="状态"><a-switch v-model="bgmForm.enabled" checked-text="启用" unchecked-text="停用" /></a-form-item></a-grid-item>
        </a-grid>
        <a-form-item label="版权及商用范围说明"><a-textarea v-model="bgmForm.copyrightNote" :max-length="255" show-word-limit placeholder="例如：已获全媒体商用授权，有效期至2027-12-31" /></a-form-item>
      </a-form>
    </a-drawer>

    <a-drawer v-model:visible="materialEditorVisible" :title="materialEditingId ? `编辑${activeMaterialMeta.label}` : `上传${activeMaterialMeta.label}`" :width="680" :ok-loading="materialSaving" @ok="saveMaterial">
      <a-alert type="info">素材上传后可按分类和标签检索，并用于后续的视频创作与复用。</a-alert>
      <a-form ref="materialFormRef" :model="materialForm" layout="vertical" class="material-form">
        <a-form-item label="素材文件" required>
          <a-upload :auto-upload="false" :limit="1" :accept="activeMaterialMeta.accept" @change="handleMaterialFile">
            <template #upload-button><a-button :loading="materialUploading"><IconUpload /> {{ materialForm.originalFileName || `选择${activeMaterialMeta.label}` }}</a-button></template>
          </a-upload>
          <div class="form-helper">{{ activeMaterialMeta.uploadHint }}</div>
        </a-form-item>
        <a-form-item field="materialName" label="素材名称" required :rules="[{ required: true, message: '请输入素材名称' }]">
          <a-input v-model="materialForm.materialName" :max-length="128" show-word-limit />
        </a-form-item>
        <a-grid :cols="2" :col-gap="16">
          <a-grid-item><a-form-item field="category" label="素材分类" required :rules="[{ required: true, message: '请选择素材分类' }]"><a-select v-model="materialForm.category"><a-option v-for="item in activeMaterialMeta.categories" :key="item" :value="item">{{ item }}</a-option></a-select></a-form-item></a-grid-item>
          <a-grid-item><a-form-item label="版权状态" required><a-select v-model="materialForm.copyrightStatus"><a-option v-for="item in bgmCopyrightStatuses" :key="item" :value="item">{{ item }}</a-option></a-select></a-form-item></a-grid-item>
        </a-grid>
        <a-form-item label="素材标签"><a-input v-model="materialForm.tags" placeholder="多个标签用逗号分隔，如：中秋, 门店, 礼盒" :max-length="500" /></a-form-item>
        <a-form-item label="素材说明"><a-textarea v-model="materialForm.description" :max-length="1000" show-word-limit placeholder="说明内容、场景和推荐用途" /></a-form-item>
        <a-form-item label="版权及商用范围说明"><a-textarea v-model="materialForm.copyrightNote" :max-length="255" show-word-limit /></a-form-item>
        <a-form-item label="状态"><a-switch v-model="materialForm.enabled" checked-text="启用" unchecked-text="停用" /></a-form-item>
      </a-form>
    </a-drawer>

    <a-modal v-model:visible="folderEditorVisible" :title="folderEditingId ? '重命名文件夹' : '新建文件夹'" width="480px" :ok-loading="folderSaving" @ok="saveMaterialFolder">
      <a-form ref="folderFormRef" :model="folderForm" layout="vertical">
        <a-form-item field="folderName" label="文件夹名称" required :rules="[{ required: true, message: '请输入文件夹名称' }, { maxLength: 128, message: '最多128个字符' }]">
          <a-input v-model="folderForm.folderName" :max-length="128" show-word-limit placeholder="例如：产品主图" />
        </a-form-item>
        <a-grid :cols="2" :col-gap="16">
          <a-grid-item>
            <a-form-item field="enabled" label="状态">
              <a-radio-group v-model="folderForm.enabled">
                <a-radio :value="true">启用</a-radio>
                <a-radio :value="false">停用</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-grid-item>
          <a-grid-item>
            <a-form-item field="sortOrder" label="排序">
              <a-input-number v-model="folderForm.sortOrder" :min="0" :max="9999" placeholder="数值越小越靠前" />
            </a-form-item>
          </a-grid-item>
        </a-grid>
      </a-form>
    </a-modal>

    <a-modal v-model:visible="moveVisible" title="移动素材" width="440px" :ok-loading="moving" @ok="confirmMoveMaterial">
      <a-alert type="info">将素材移动到其他文件夹，移动后可在目标文件夹中查看。</a-alert>
      <a-form :model="moveForm" layout="vertical" class="move-form">
        <a-form-item label="目标文件夹" required>
          <a-select v-model="moveForm.folderId">
            <a-option :value="0">未分类</a-option>
            <a-option v-for="folder in moveFolderOptions" :key="folder.id" :value="folder.id">{{ folder.folderName }}</a-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:visible="importVisible" title="导入账号" width="620px" :ok-loading="accountImporting" @ok="finishImport">
      <a-alert type="info">请按模板填写平台、账号名称、平台账号ID、账号类型和员工工号。导入后统一进入“待校验”状态。</a-alert>
      <a-button type="text" @click="downloadAccountTemplate"><IconDownload /> 下载导入模板.csv</a-button>
      <a-upload drag :auto-upload="false" accept=".csv" :limit="1" @change="handleImportFile"><template #upload-button><div class="import-drop"><IconUpload /><strong>点击或拖拽CSV文件到此处</strong><span>支持 UTF-8 CSV，单次最多10,000条</span></div></template></a-upload>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { Message, Modal, type FormInstance, type TableColumnData } from '@arco-design/web-vue';
import {
  IconArrowRise, IconDownload, IconFolder, IconLeft, IconMoreVertical, IconPlus, IconRefresh, IconRight, IconRobot, IconSearch,
  IconUpload, IconVideoCamera
} from '@arco-design/web-vue/es/icon';
import {
  createContentAccount, createContentActivity, createContentPlan, createContentBgm, createContentMaterial, createContentMaterialFolder, deleteContentAccounts, deleteContentActivity, deleteContentBgm, deleteContentMaterial, deleteContentMaterialFolder,
  fetchActivityPlans, fetchContentAccounts, fetchContentActivities, fetchContentDeliveryTasks,
  downloadPersonnelImportTemplate, fetchContentAccountOrganizations, fetchContentAccountUsers,
  fetchContentMaterialFolders, fetchContentTaskOrganizations, fetchContentTaskUsers, fetchContentVideoPerformance, generateContentStoryboardScripts,
  generateContentShootingRequirement, generateContentVideoDescription, importContentAccounts,
  moveContentMaterial, pauseContentActivity, publishContentPlan, resumeContentActivity, terminateContentActivity, updateContentAccount, updateContentActivity,
  fetchContentBgms, fetchContentMaterials, moveContentBgm, updateContentBgm, updateContentMaterial, updateContentMaterialFolder, uploadContentBgm, uploadContentMaterial, uploadContentSampleCover, uploadContentSampleVideo, validatePersonnelImport
} from '@tql-store/api';
import { usePermission } from '@tql-store/auth';
import type { ContentAccountItem, ContentAccountPayload, ContentBgmItem, ContentBgmPayload, ContentDeliveryItem, ContentMaterialFolderItem, ContentMaterialFolderPayload, ContentMaterialFolderType, ContentMaterialItem, ContentMaterialPayload, ContentMaterialType, ContentVideoPerformanceItem, OrganizationOption, PersonnelImportResult, UserItem } from '@tql-store/shared';
import OrganizationCollapseNode from '../components/OrganizationCollapseNode.vue';

type ModuleKey = 'plans' | 'calendar' | 'analytics' | 'accounts' | 'bgm';
type PlanRow = {
  id: string | number; type: string; name: string; owner: string; status: string;
  accounts: number; completed: number; progress: number; personProgress: number;
  completedVideos: number; totalVideos: number; videoProgress: number;
  startDate: string; endDate: string; startTime: string; tone?: string;
  objective?: string; rawStatus?: string;
};
type CalendarTone = 'draft' | 'active' | 'paused' | 'terminated' | 'completed' | 'expired';
type UploadFileEntry = File | { file?: File; originFile?: File; raw?: File };

function getUploadFile(...sources: unknown[]): File | undefined {
  for (const source of sources) {
    if (!source) continue;
    if (Array.isArray(source)) {
      const file = getUploadFile(...source);
      if (file) return file;
      continue;
    }
    if (source instanceof File) return source;
    if (typeof source === 'object') {
      const entry = source as { file?: unknown; originFile?: unknown; raw?: unknown };
      const file = getUploadFile(entry.file, entry.originFile, entry.raw);
      if (file) return file;
    }
  }
  return undefined;
}

const activityStatusMeta: Record<string, { label: string; tone: CalendarTone }> = {
  DRAFT: { label: '草稿', tone: 'draft' },
  ACTIVE: { label: '进行中', tone: 'active' },
  PAUSED: { label: '已暂停', tone: 'paused' },
  TERMINATED: { label: '已终止', tone: 'terminated' },
  COMPLETED: { label: '已完结', tone: 'completed' },
  ENDED: { label: '已完结', tone: 'completed' },
  EXPIRED: { label: '已完结', tone: 'expired' }
};

const props = withDefaults(defineProps<{ module?: ModuleKey }>(), { module: 'plans' });
const activeModule = computed(() => props.module);
const { can } = usePermission();
const P = {
  planView: 'merchant:content:view',
  planQuery: 'merchant:content:plan:query',
  planCreate: 'merchant:content:plan:create',
  planSave: 'merchant:content:plan:save',
  planPublish: 'merchant:content:plan:publish',
  planDelivery: 'merchant:content:plan:delivery:view',
  planCancel: 'merchant:content:plan:cancel',
  planUpdate: 'merchant:content:plan:update',
  planExportDelivery: 'merchant:content:plan:delivery:export',
  planExportMaterial: 'merchant:content:plan:material:export',
  planSelectEmployee: 'merchant:content:plan:employee:select',
  planImportEmployee: 'merchant:content:plan:employee:import',
  planValidateEmployee: 'merchant:content:plan:employee:validate',
  planAiScript: 'merchant:content:plan:script:generate',
  calendarView: 'merchant:content:calendar:view',
  calendarCreate: 'merchant:content:calendar:create',
  analyticsQuery: 'merchant:content:analytics:query',
  accountQuery: 'merchant:content:account:query',
  accountView: 'merchant:content:account:view',
  accountDetailView: 'merchant:content:account:detail:view',
  accountImport: 'merchant:content:account:import',
  accountExport: 'merchant:content:account:export',
  accountCreate: 'merchant:content:account:create',
  accountUpdate: 'merchant:content:account:update',
  accountDelete: 'merchant:content:account:delete',
  bgmView: 'merchant:content:bgm:view',
  bgmCreate: 'merchant:content:bgm:create',
  bgmUpdate: 'merchant:content:bgm:update',
  bgmDelete: 'merchant:content:bgm:delete'
} as const;
const loading = ref(false);
const submitting = ref(false);
const filters = reactive({ type: '', keyword: '', status: '', owner: '', dateRange: [] as string[] });
const appliedFilters = reactive({ ...filters });
const backendPlans = ref<PlanRow[]>([]);
const statusClock = ref(Date.now());
let statusClockTimer: ReturnType<typeof setInterval> | undefined;
const allPlans = computed(() => backendPlans.value.map(plan => {
  const waiting = plan.rawStatus === 'ACTIVE' && new Date(plan.startTime).getTime() > statusClock.value;
  const statusMeta = waiting
    ? { label: '待开始', tone: 'draft' as CalendarTone }
    : activityStatusMeta[plan.rawStatus || ''] || { label: plan.rawStatus || plan.status, tone: 'draft' as CalendarTone };
  return { ...plan, status: statusMeta.label, tone: statusMeta.tone };
}));
const visiblePlans = computed(() => allPlans.value.filter(plan =>
  (!appliedFilters.type || plan.type === appliedFilters.type) &&
  (!appliedFilters.keyword || plan.name.includes(appliedFilters.keyword)) &&
  (!appliedFilters.status || plan.status === appliedFilters.status) &&
  (!appliedFilters.owner || plan.owner.includes(appliedFilters.owner))
));
const planColumns: TableColumnData[] = [
  { title: '计划名称', dataIndex: 'name', width: 230 }, { title: '计划类型', slotName: 'type', width: 100 },
  { title: '下发状态', slotName: 'status', width: 110 },
  { title: '创建人', dataIndex: 'owner', width: 100 }, { title: '发布日期', slotName: 'period', width: 150 },
  { title: '选定账号数', dataIndex: 'accounts', width: 110 },
  { title: '执行进度(人员)', slotName: 'personProgress', width: 180 },
  { title: '执行进度(视频)', slotName: 'videoProgress', width: 180 },
  { title: '操作', slotName: 'actions', width: 240, fixed: 'right' }
];

async function loadPlans() {
  loading.value = true;
  try {
    const rows = await fetchContentActivities();
    backendPlans.value = rows.map(item => {
      const statusMeta = activityStatusMeta[item.status] || { label: item.status, tone: 'draft' as CalendarTone };
      return {
        id: item.id, type: item.creationMode === 'SELF_CREATED' ? '原创' : '半原创', name: item.name,
        owner: item.ownerName, status: statusMeta.label,
        objective: item.objective, rawStatus: item.status,
        accounts: item.employeeCount, completed: item.completedCount,
        progress: item.employeeCount ? Math.round(item.completedCount / item.employeeCount * 100) : 0,
        personProgress: item.employeeCount ? Math.round(item.completedCount / item.employeeCount * 100) : 0,
        completedVideos: item.completedVideoCount || 0,
        totalVideos: item.totalVideoCount || 0,
        videoProgress: item.totalVideoCount ? Math.min(100, Math.round(item.completedVideoCount / item.totalVideoCount * 100)) : 0,
        startDate: item.releaseStartTime.slice(0, 10), endDate: item.endTime.slice(0, 10), startTime: item.startTime,
        tone: statusMeta.tone
      };
    });
  } catch {
    backendPlans.value = [];
    Message.error('发布计划加载失败，请检查后台服务');
  } finally { loading.value = false; }
}
function applyFilters() { Object.assign(appliedFilters, filters); }
function resetFilters() { Object.assign(filters, { id: '', type: '', keyword: '', status: '', owner: '', dateRange: [] }); Object.assign(appliedFilters, filters); }
function statusClass(status: string) {
  return status === '进行中' ? 'status-active' : status === '已终止' ? 'status-terminated' : status === '已完结' ? 'status-done' : 'status-draft';
}
function statusColor(status: string) {
  return ({ '进行中': 'arcoblue', '已暂停': 'orange', '已终止': 'red', '已完结': 'green', '草稿': 'orange' } as Record<string, string>)[status] || 'gray';
}
function calendarStatusColor(tone?: string) {
  return ({ active: 'blue', paused: 'orange', terminated: 'red', completed: 'green', expired: 'gray', draft: 'orange' } as Record<string, string>)[tone || 'draft'];
}
function csvCell(value: unknown) {
  return `"${String(value ?? '').replaceAll('"', '""')}"`;
}
function exportDetails() {
  if (!deliveryRows.value.length) {
    Message.warning('当前计划没有可导出的人员');
    return;
  }
  const lines = [
    ['员工工号', '发布人', '组织', '任务起始时间', '执行状态'],
    ...deliveryRows.value.map(row => [row.employeeNo, row.name, row.org, `${row.periodStart} 至 ${row.periodEnd}`, row.status])
  ];
  const blob = new Blob([`\uFEFF${lines.map(line => line.map(csvCell).join(',')).join('\r\n')}`], {
    type: 'text/csv;charset=utf-8'
  });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = `人员-${selectedPlan.value?.name || selectedPlan.value?.id || '计划'}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}
function exportMaterials() { Message.success('素材收集表已生成，正在下载'); }

const createVisible = ref(false);
const editingActivityId = ref<number>();
const wizardStep = ref(1);
type PublishPlatform = '抖音' | '小红书' | '视频号';
type PlatformQuota = { total: number; daily: number };
const supportedPublishPlatforms: PublishPlatform[] = ['抖音', '小红书', '视频号'];
const createPlatformQuotas = (): Record<PublishPlatform, PlatformQuota> => ({
  抖音: { total: 1, daily: 1 },
  小红书: { total: 1, daily: 1 },
  视频号: { total: 1, daily: 1 }
});
const planForm = reactive({
  name: '', deliveryMode: '员工任务', type: '半原创', dateRange: [] as string[], accountMode: '账号',
  employeeCount: 0, platforms: ['抖音'] as PublishPlatform[], platformQuotas: createPlatformQuotas(), sampleAspect: 'portrait' as 'portrait' | 'landscape',
  coverExampleUrl: '', coverExampleName: '', coverUploading: false,
  bgmIds: [] as number[], voiceStyle: '',
  description: '', taskCopy: '', topic: '', title: '', taskStartTime: '', immediateStart: false,
  storyboardCount: 3, originalRequirement: ''
});
const topicInput = ref('');
const topicList = ref<string[]>([]);
const formattedTopics = computed(() => topicList.value.map(topic => `#${topic}#`).join(' '));
const platformQuotaSummary = computed(() => planForm.platforms.map(platform => {
  const quota = planForm.platformQuotas[platform];
  return `${platform} ${quota.total}条（每天可发送${quota.daily}条）`;
}).join('、'));

function normalizeTopic(value: string) {
  return value.trim().replace(/^#+|#+$/g, '').replace(/\s+/g, '');
}

function syncTopicValue() {
  planForm.topic = formattedTopics.value;
}

function addTopic() {
  const topic = normalizeTopic(topicInput.value);
  if (!topic) {
    Message.warning('请输入有效的话题名称');
    return;
  }
  if (topicList.value.some(item => item.toLocaleLowerCase() === topic.toLocaleLowerCase())) {
    Message.warning('该话题已添加');
    return;
  }
  if (topicList.value.length >= 10) {
    Message.warning('最多添加 10 个话题');
    return;
  }
  topicList.value.push(topic);
  topicInput.value = '';
  syncTopicValue();
}

function removeTopic(topic: string) {
  topicList.value = topicList.value.filter(item => item !== topic);
  syncTopicValue();
}

function parseTopics(value: string) {
  const standardTopics = [...value.matchAll(/#([^#]+)#/g)].map(match => normalizeTopic(match[1]));
  const source = standardTopics.length ? standardTopics : value.split(/[\s,，]+/).map(normalizeTopic);
  return [...new Set(source.filter(Boolean))].slice(0, 10);
}
type StoryboardDraft = {
  requirement: string;
  script: string;
  minDuration: number;
  maxDuration: number;
  sampleVideoUrl?: string;
  sampleVideoName?: string;
  sampleAspect?: 'portrait' | 'landscape' | '';
  uploading?: boolean;
};
const emptyStoryboard = (): StoryboardDraft => ({
  requirement: '',
  script: '',
  minDuration: 5,
  maxDuration: 15,
  sampleVideoUrl: '',
  sampleVideoName: '',
  sampleAspect: 'portrait',
  uploading: false
});
const storyboards = ref<StoryboardDraft[]>([emptyStoryboard(), emptyStoryboard(), emptyStoryboard()]);
const activeStoryboard = ref(0);
const activeConfigTab = ref<'basic' | 'content' | 'material'>('basic');
const generating = ref(false);
const descriptionAiVisible = ref(false);
const descriptionPrompt = ref('');
const generatingDescription = ref(false);
const generatingShootingRequirement = ref(false);
const planBgmOptions = ref<ContentBgmItem[]>([]);
const planBgmLoading = ref(false);
const selectedPlanBgms = computed(() => planForm.bgmIds
  .map(id => planBgmOptions.value.find(item => item.id === id))
  .filter((item): item is ContentBgmItem => Boolean(item)));
const voiceStyleOptions = ['自然女声', '温柔女声', '活力女声', '知性女声', '沉稳男声', '磁性男声', '活力男声', '新闻播报'];
async function loadPlanBgmOptions() {
  planBgmLoading.value = true;
  try { planBgmOptions.value = await fetchContentBgms({ enabled: true }); }
  catch (error) { Message.error(error instanceof Error ? error.message : '可用BGM加载失败'); }
  finally { planBgmLoading.value = false; }
}
function openCreate(date?: string) { editingActivityId.value = undefined; resetPlanForm(); wizardStep.value = 1; if (date) planForm.dateRange = [date, date]; createVisible.value = true; loadEmployees(); void loadPlanBgmOptions(); }
function leaveCreate() { createVisible.value = false; editingActivityId.value = undefined; wizardStep.value = 1; }
function forwardWizardWheel(event: WheelEvent) {
  const shell = event.currentTarget as HTMLElement | null;
  const content = shell?.querySelector<HTMLElement>('.wizard-content');
  if (!shell || !content || content.contains(event.target as Node)) return;
  content.scrollTop += event.deltaY;
}
function resetPlanForm() { Object.assign(planForm, { name: '', deliveryMode: '员工任务', type: '半原创', dateRange: [], accountMode: '账号', employeeCount: 0, platforms: ['抖音'] as PublishPlatform[], platformQuotas: createPlatformQuotas(), sampleAspect: 'portrait', coverExampleUrl: '', coverExampleName: '', coverUploading: false, bgmIds: [] as number[], voiceStyle: '', description: '', taskCopy: '', topic: '', title: '', taskStartTime: '', immediateStart: false, storyboardCount: 3, originalRequirement: '' }); topicInput.value = ''; topicList.value = []; activeConfigTab.value = 'basic'; activeStoryboard.value = 0; selectedEmployeeIds.value = []; selectedPlanAccountIds.value = []; importedPersonnelDetails.value = {}; storyboards.value = [emptyStoryboard(), emptyStoryboard(), emptyStoryboard()]; }
function instructionValue(lines: string[], label: string) {
  return lines.find(line => line.startsWith(`${label}：`))?.slice(label.length + 1) || '';
}
function parsePlatformQuotas(value: string) {
  const quotas = createPlatformQuotas();
  value.split('、').filter(Boolean).forEach(item => {
    const [platform, values] = item.split('=');
    if (!supportedPublishPlatforms.includes(platform as PublishPlatform)) return;
    const [total, daily] = (values || '').split('/').map(Number);
    quotas[platform as PublishPlatform] = {
      total: Number.isInteger(total) && total > 0 ? total : 1,
      daily: Number.isInteger(daily) && daily > 0 ? daily : 1
    };
  });
  return quotas;
}
async function openEdit(record: PlanRow) {
  editingActivityId.value = Number(record.id);
  resetPlanForm();
  submitting.value = true;
  try {
    await Promise.all([loadEmployees(), loadAccounts(), loadPlanBgmOptions()]);
    const [plans, personnel] = await Promise.all([
      fetchActivityPlans(Number(record.id)),
      fetchContentDeliveryTasks(Number(record.id))
    ]);
    const plan = plans[0];
    if (!plan) throw new Error('计划内容不存在');
    const lines = plan.taskInstruction.split('\n').filter(Boolean);
    const platforms = instructionValue(lines, '发布平台')
      .split('、')
      .filter((platform): platform is PublishPlatform => supportedPublishPlatforms.includes(platform as PublishPlatform));
    const platformQuotas = parsePlatformQuotas(instructionValue(lines, '平台发布配置'));
    const storyboardText = instructionValue(lines, '分镜要求');
    const parsedStoryboards = storyboardText
      ? storyboardText.split(' | ').map(item => {
          const segments = item.replace(/^\d+\./, '').split('；');
          const requirement = segments.shift() || '';
          const values = Object.fromEntries(segments.map(segment => {
            const separator = segment.indexOf('：');
            return separator < 0
              ? [segment, '']
              : [segment.slice(0, separator), segment.slice(separator + 1)];
          }));
          return {
            ...emptyStoryboard(),
            requirement,
            script: values['台词'] || '',
            minDuration: Number(values['时长要求']?.match(/^(\d+)/)?.[1]) || 5,
            maxDuration: Number(values['时长要求']?.match(/～(\d+)/)?.[1]) || 15,
            sampleVideoUrl: values['样例视频'] || '',
            sampleVideoName: values['样例视频'] ? '已上传样例视频' : '',
            sampleAspect: normalizeSampleAspect(values['样例比例']) || 'portrait'
          };
        })
      : [];
    Object.assign(planForm, {
      name: record.name,
      type: plan.creationMode === 'SELF_CREATED' ? '原创' : '半原创',
      dateRange: [record.startDate, record.endDate],
      taskStartTime: record.startTime.replace('T', ' ').slice(0, 19),
      immediateStart: instructionValue(lines, '发布即开始') === '是',
      accountMode: '账号',
      employeeCount: new Set(personnel.map(item => item.employeeId)).size,
      platforms: platforms.length ? platforms : ['抖音'],
      platformQuotas,
      sampleAspect: parsedStoryboards[0]?.sampleAspect || 'portrait',
      coverExampleUrl: instructionValue(lines, '视频封面示例'),
      coverExampleName: instructionValue(lines, '视频封面示例') ? '已上传视频封面示例' : '',
      bgmIds: (instructionValue(lines, '参考BGM IDs') || instructionValue(lines, '参考BGM ID'))
        .split(/[、,，]/).map(Number).filter(id => Number.isInteger(id) && id > 0),
      voiceStyle: instructionValue(lines, '建议配音'),
      description: record.objective || '',
      taskCopy: lines[0] || '',
      topic: instructionValue(lines, '发布话题'),
      title: instructionValue(lines, '发布标题'),
      storyboardCount: plan.storyboardCount,
      originalRequirement: instructionValue(lines, '拍摄要求')
    });
    topicList.value = parseTopics(planForm.topic);
    syncTopicValue();
    selectedEmployeeIds.value = [...new Set(personnel.map(item => item.employeeId))];
    selectedPlanAccountIds.value = accounts.value
      .filter(account => selectedEmployeeIds.value.includes(account.employeeId))
      .map(account => account.id);
    storyboards.value = parsedStoryboards.length
      ? parsedStoryboards
      : Array.from({ length: Math.max(plan.storyboardCount, 1) }, emptyStoryboard);
    wizardStep.value = 1;
    createVisible.value = true;
  } catch (error) {
    editingActivityId.value = undefined;
    Message.error(error instanceof Error ? error.message : '计划加载失败');
  } finally {
    submitting.value = false;
  }
}
function syncStoryboards(value?: number) { const count = value || planForm.storyboardCount; while (storyboards.value.length < count) storyboards.value.push({ ...emptyStoryboard(), sampleAspect: planForm.sampleAspect }); storyboards.value = storyboards.value.slice(0, count); if (activeStoryboard.value >= count) activeStoryboard.value = count - 1; }
function handleStoryboardAspectChange(value: string | number | boolean) {
  if (value !== 'portrait' && value !== 'landscape') return;
  let cleared = false;
  storyboards.value.forEach(storyboard => {
    if (storyboard.sampleVideoUrl && storyboard.sampleAspect !== value) {
      storyboard.sampleVideoUrl = '';
      storyboard.sampleVideoName = '';
      cleared = true;
    }
    storyboard.sampleAspect = value;
  });
  if (cleared) Message.warning('画面方向已变更，不符合新方向的样例视频已清除，请重新上传');
  if (planForm.coverExampleUrl) {
    planForm.coverExampleUrl = '';
    planForm.coverExampleName = '';
    Message.warning('画面方向已变更，视频封面示例已清除，请重新上传');
  }
}
function openDescriptionGenerator() {
  descriptionPrompt.value = planForm.description;
  descriptionAiVisible.value = true;
}
async function generateVideoDescription(): Promise<boolean> {
  const prompt = descriptionPrompt.value.trim();
  if (!prompt || generatingDescription.value) return false;
  generatingDescription.value = true;
  try {
    const result = await generateContentVideoDescription(prompt);
    planForm.description = result.text;
    Message.success('视频内容描述已生成并填入，可继续修改');
    return true;
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '视频内容描述生成失败');
    return false;
  } finally {
    generatingDescription.value = false;
  }
}
async function generateScripts() {
  if (!planForm.description) { Message.warning('请先填写视频内容描述'); return; }
  if (generating.value) return;
  generating.value = true;
  try {
    syncStoryboards(planForm.storyboardCount);
    const result = await generateContentStoryboardScripts(planForm.description, storyboards.value.length);
    storyboards.value.forEach((item, index) => { item.script = result.scripts[index] || ''; });
    Message.success('全部分镜台词已生成，可继续修改');
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '分镜台词生成失败');
  } finally {
    generating.value = false;
  }
}
async function generateShootingRequirement() {
  const description = planForm.description.trim();
  if (!description) { Message.warning('请先填写视频内容描述'); return; }
  if (generatingShootingRequirement.value) return;
  generatingShootingRequirement.value = true;
  try {
    const result = await generateContentShootingRequirement(description);
    planForm.originalRequirement = result.text;
    Message.success('原创拍摄要求已生成并填入，可继续修改');
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '原创拍摄要求生成失败');
  } finally {
    generatingShootingRequirement.value = false;
  }
}
function estimateDuration(script: string) { return Math.max(0, Math.ceil(script.length / 4)); }
function minimumReleaseTime() {
  return Date.now() + 3 * 24 * 60 * 60 * 1000;
}
function disabledReleaseDate(date: Date) {
  if (editingActivityId.value) return false;
  return date.getTime() <= minimumReleaseTime();
}
function taskStartBounds() {
  const now = new Date();
  const publishStart = planForm.dateRange[0]
    ? new Date(`${planForm.dateRange[0]}T00:00:00`)
    : now;
  const min = now;
  const max = planForm.dateRange[0]
    ? new Date(publishStart.getTime() - 1)
    : undefined;
  return { min, max };
}
function sameLocalDate(left: Date, right: Date) {
  return left.getFullYear() === right.getFullYear()
    && left.getMonth() === right.getMonth()
    && left.getDate() === right.getDate();
}
function disabledTaskStartDate(date?: Date) {
  if (editingActivityId.value) return false;
  if (!date) return false;
  const { min, max } = taskStartBounds();
  const dayStart = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
  const minDay = new Date(min.getFullYear(), min.getMonth(), min.getDate()).getTime();
  const maxDay = max ? new Date(max.getFullYear(), max.getMonth(), max.getDate()).getTime() : undefined;
  return dayStart < minDay || (maxDay !== undefined && dayStart > maxDay);
}
function disabledTaskStartTime(date?: Date) {
  if (editingActivityId.value) return {};
  if (!date) return {};
  const { min } = taskStartBounds();
  if (!sameLocalDate(date, min)) return {};
  const selectedHour = date.getHours();
  const selectedMinute = date.getMinutes();
  return {
    disabledHours: () => Array.from({ length: min.getHours() }, (_, index) => index),
    disabledMinutes: () => selectedHour === min.getHours()
      ? Array.from({ length: min.getMinutes() }, (_, index) => index)
      : [],
    disabledSeconds: () => selectedHour === min.getHours() && selectedMinute === min.getMinutes()
      ? Array.from({ length: min.getSeconds() + 1 }, (_, index) => index)
      : []
  };
}
function taskStartIsValid() {
  if (planForm.immediateStart) return true;
  if (!planForm.taskStartTime) return false;
  if (editingActivityId.value) return true;
  const value = new Date(planForm.taskStartTime.replace(' ', 'T'));
  const { min, max } = taskStartBounds();
  return !Number.isNaN(value.getTime())
    && value.getTime() >= min.getTime()
    && (!max || value.getTime() <= max.getTime());
}
function handleImmediateStartChange(checked: boolean | (string | number | boolean)[]) {
  if (checked === true) {
    planForm.taskStartTime = '';
    return;
  }
  if (planForm.dateRange.length && !planForm.taskStartTime) {
    planForm.taskStartTime = formatLocalDateTime(new Date(Date.now() + 60 * 60 * 1000));
  }
}
function validateTaskStartTime() {
  if (!planForm.taskStartTime || taskStartIsValid()) return;
  planForm.taskStartTime = '';
  Message.warning('任务开始时间只能选择当前时间到发布日前一天');
}
function releaseDateIsValid() {
  if (planForm.dateRange.length !== 2) return false;
  if (editingActivityId.value) return true;
  return new Date(`${planForm.dateRange[0]}T00:00:00`).getTime() > minimumReleaseTime();
}
function validateReleaseDateSelection() {
  if (!planForm.dateRange.length) return;
  if (editingActivityId.value) return;
  if (!releaseDateIsValid()) {
    planForm.dateRange = [];
    planForm.taskStartTime = '';
    Message.warning('为确保流程正常进行，发布日期必须大于当前时间3天');
    return;
  }
  if (!planForm.immediateStart && !planForm.taskStartTime) {
    const defaultStart = new Date(Date.now() + 60 * 60 * 1000);
    planForm.taskStartTime = formatLocalDateTime(defaultStart);
  }
}
function formatLocalDateTime(value: Date) {
  const part = (number: number) => String(number).padStart(2, '0');
  return `${value.getFullYear()}-${part(value.getMonth() + 1)}-${part(value.getDate())} ${part(value.getHours())}:${part(value.getMinutes())}:${part(value.getSeconds())}`;
}
async function uploadStoryboardSample(index: number, files: UploadFileEntry[], fileItem?: UploadFileEntry) {
  const storyboard = storyboards.value[index];
  const file = getUploadFile(fileItem, files);
  if (!storyboard || !file) {
    Message.warning('未能读取所选视频，请重新选择文件');
    return;
  }
  if (file.type !== 'video/mp4' && !file.name.toLowerCase().endsWith('.mp4')) {
    Message.warning('样例视频仅支持 MP4 格式');
    return;
  }
  if (file.size > 200 * 1024 * 1024) {
    Message.warning('样例视频不能超过 200MB');
    return;
  }
  storyboard.uploading = true;
  try {
    const aspect = await readVideoAspect(file);
    if (planForm.sampleAspect !== aspect) {
      Message.warning(`当前统一设置为${sampleAspectText(planForm.sampleAspect)}，请上传对应比例的视频`);
      return;
    }
    const result = await uploadContentSampleVideo(file);
    storyboard.sampleVideoUrl = result.url;
    storyboard.sampleVideoName = result.originalName || file.name;
    storyboard.sampleAspect = aspect;
    Message.success(`分镜 ${index + 1} 样例视频上传成功，已识别为${sampleAspectText(aspect)}`);
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '样例视频上传失败');
  } finally {
    storyboard.uploading = false;
  }
}
async function uploadCoverExample(files: UploadFileEntry[], fileItem?: UploadFileEntry) {
  const file = getUploadFile(fileItem, files);
  if (!file) { Message.warning('未能读取所选图片，请重新选择文件'); return; }
  if (!/\.(jpe?g|png|webp)$/i.test(file.name)) { Message.warning('封面示例仅支持 JPG、PNG、WEBP 格式'); return; }
  if (file.size > 10 * 1024 * 1024) { Message.warning('封面示例不能超过 10MB'); return; }
  planForm.coverUploading = true;
  try {
    const aspect = await readImageAspect(file);
    if (planForm.sampleAspect !== aspect) {
      Message.warning(`当前统一设置为${sampleAspectText(planForm.sampleAspect)}，请上传对应比例的封面图片`);
      return;
    }
    const result = await uploadContentSampleCover(file);
    planForm.coverExampleUrl = result.url;
    planForm.coverExampleName = result.originalName || file.name;
    Message.success('视频封面模板示例上传成功，员工 App 将同步展示');
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '视频封面示例上传失败');
  } finally {
    planForm.coverUploading = false;
  }
}
function readImageAspect(file: File): Promise<'portrait' | 'landscape'> {
  return new Promise(resolve => {
    const url = URL.createObjectURL(file);
    const image = new Image();
    const finish = (aspect: 'portrait' | 'landscape') => { URL.revokeObjectURL(url); resolve(aspect); };
    image.onload = () => finish(image.naturalWidth > image.naturalHeight ? 'landscape' : 'portrait');
    image.onerror = () => finish('portrait');
    image.src = url;
  });
}
function readVideoAspect(file: File): Promise<'portrait' | 'landscape'> {
  return new Promise(resolve => {
    const url = URL.createObjectURL(file);
    const video = document.createElement('video');
    const finish = (aspect: 'portrait' | 'landscape') => {
      URL.revokeObjectURL(url);
      resolve(aspect);
    };
    video.preload = 'metadata';
    video.onloadedmetadata = () => {
      const width = video.videoWidth || 0;
      const height = video.videoHeight || 0;
      finish(width > height ? 'landscape' : 'portrait');
    };
    video.onerror = () => finish('portrait');
    video.src = url;
  });
}
function normalizeSampleAspect(value?: string): 'portrait' | 'landscape' | '' {
  const text = String(value || '').toLowerCase();
  if (/16\s*[:：]\s*9|landscape|horizontal|横/.test(text)) return 'landscape';
  if (/9\s*[:：]\s*16|portrait|vertical|竖/.test(text)) return 'portrait';
  return '';
}
function sampleAspectText(value?: string) {
  return value === 'landscape' ? '横版 16:9' : '竖版 9:16';
}
function storyboardValidationMessage() {
  const index = storyboards.value.findIndex(item => !item.requirement || !item.script || !item.sampleVideoUrl || !Number.isInteger(item.minDuration) || !Number.isInteger(item.maxDuration) || item.minDuration < 1 || item.maxDuration < item.minDuration);
  if (index < 0) return '';
  const storyboard = storyboards.value[index];
  const missing = [
    !storyboard.requirement && '拍摄要求',
    (!Number.isInteger(storyboard.minDuration) || !Number.isInteger(storyboard.maxDuration) || storyboard.minDuration < 1 || storyboard.maxDuration < storyboard.minDuration) && '分镜时长要求',
    !storyboard.script && '分镜台词',
    !storyboard.sampleVideoUrl && '样例视频'
  ].filter(Boolean).join('、');
  return `请完善分镜 ${index + 1} 的${missing}`;
}
function nextWizardStep() {
  if (wizardStep.value === 1 && !planForm.name) {
    Message.warning('请填写计划名称');
    return;
  }
  if (wizardStep.value === 1 && planForm.name.length > 30) {
    Message.warning('计划名称不能超过30字');
    return;
  }
  if (wizardStep.value === 2) {
    if (!planForm.dateRange.length || !releaseDateIsValid() || !taskStartIsValid() || !planForm.platforms.length) {
      Message.warning('请完善发布日期、任务开始时间和发布平台，且发布日期必须大于当前时间3天');
      return;
    }
    if (!platformQuotasAreValid()) {
      Message.warning('请填写每个平台的发布总条数和每天可发送条数，且均需大于0');
      return;
    }
    syncStoryboards(planForm.storyboardCount);
  }
  if (wizardStep.value === 3) {
    if (!planForm.description) {
      Message.warning('请填写视频内容描述');
      return;
    }
    const storyboardError = planForm.type === '半原创' ? storyboardValidationMessage() : '';
    if (storyboardError) {
      activeConfigTab.value = 'content';
      Message.warning(storyboardError);
      return;
    }
    if (planForm.type === '原创' && !planForm.originalRequirement) {
      Message.warning('请填写原创拍摄要求');
      return;
    }
  }
  wizardStep.value++;
}
function validatePlan() {
  if (!planForm.name || !planForm.dateRange.length || !releaseDateIsValid() || !taskStartIsValid() || !planForm.description || !planForm.platforms.length) {
    Message.warning('请完善活动信息，且发布日期必须大于当前时间3天');
    return false;
  }
  if (!platformQuotasAreValid()) { Message.warning('请完善每个平台的视频条数配置'); return false; }
  if (!selectedEmployeeIds.value.length) { Message.warning(planForm.accountMode === '账号' ? '请至少选择一个发布账号' : '请至少选择一名员工'); return false; }
  const storyboardError = planForm.type === '半原创' ? storyboardValidationMessage() : '';
  if (storyboardError) { activeConfigTab.value = 'content'; Message.warning(storyboardError); return false; }
  return true;
}
function toLocalDateTime(date: string, end = false) { return `${date}T${end ? '23:59:59' : '00:00:00'}`; }
function platformQuotasAreValid() {
  return planForm.platforms.length > 0 && planForm.platforms.every(platform => {
    const quota = planForm.platformQuotas[platform];
    return Number.isInteger(quota.total) && quota.total > 0 && Number.isInteger(quota.daily) && quota.daily > 0;
  });
}
function buildTaskInstruction() {
  return [
    planForm.description,
    planForm.immediateStart && '发布即开始：是',
    planForm.topic && `发布话题：${planForm.topic}`,
    `发布平台：${planForm.platforms.join('、')}`,
    `平台发布配置：${planForm.platforms.map(platform => {
      const quota = planForm.platformQuotas[platform];
      return `${platform}=${quota.total}/${quota.daily}`;
    }).join('、')}`,
    planForm.type === '半原创' && planForm.coverExampleUrl && `视频封面示例：${planForm.coverExampleUrl}`,
    planForm.type === '半原创' && planForm.coverExampleUrl && `视频封面比例：${sampleAspectText(planForm.sampleAspect)}`,
    planForm.type === '半原创' && planForm.voiceStyle && `建议配音：${planForm.voiceStyle}`,
    planForm.type === '半原创' && selectedPlanBgms.value.length && `参考BGM IDs：${selectedPlanBgms.value.map(item => item.id).join('、')}`,
    ...(planForm.type === '半原创'
      ? selectedPlanBgms.value.map((item, index) => `参考BGM ${index + 1}：${item.bgmName}｜${item.fileUrl}｜${item.videoType} / ${item.mood}`)
      : []),
    planForm.type === '原创'
      ? `拍摄要求：${planForm.originalRequirement}`
      : `分镜要求：${storyboards.value.map((item, index) => [
        `${index + 1}.${item.requirement}`,
        `时长要求：${item.minDuration}～${item.maxDuration}秒`,
        `台词：${item.script}`,
        item.sampleVideoUrl && `样例视频：${item.sampleVideoUrl}`,
        `样例比例：${sampleAspectText(planForm.sampleAspect)}`
      ].filter(Boolean).join('；')).join(' | ')}`
  ].filter(Boolean).join('\n').slice(0, 1000);
}
async function persistPlan(publish: boolean, commitEdit = false) {
  const [startDate, endDate] = planForm.dateRange;
  const taskInstruction = buildTaskInstruction();
  const effectiveStartTime = planForm.immediateStart
    ? formatLocalDateTime(new Date(Date.now() + (publish ? 5000 : 60 * 60 * 1000)))
    : planForm.taskStartTime;
  if (editingActivityId.value) {
    if (!commitEdit || wizardStep.value !== 4) {
      throw new Error('编辑内容尚未到最终确认步骤，不能更新计划');
    }
    await updateContentActivity(editingActivityId.value, {
      name: planForm.name,
      objective: planForm.description,
      startTime: effectiveStartTime.replace(' ', 'T'),
      releaseStartTime: toLocalDateTime(startDate),
      endTime: toLocalDateTime(endDate, true),
      taskInstruction,
      creationMode: planForm.type === '原创' ? 'SELF_CREATED' : 'AI_ASSISTED',
      storyboardCount: planForm.type === '原创' ? 1 : planForm.storyboardCount,
      trainingPolicy: 'NONE',
      platforms: planForm.platforms,
      employeeIds: selectedEmployeeIds.value
    });
    return;
  }
  const activityId = await createContentActivity({
    name: planForm.name,
    objective: planForm.description,
    startTime: effectiveStartTime.replace(' ', 'T'),
    releaseStartTime: toLocalDateTime(startDate),
    endTime: toLocalDateTime(endDate, true)
  });
  const planId = await createContentPlan({
    activityId, name: planForm.name, taskInstruction,
    creationMode: planForm.type === '原创' ? 'SELF_CREATED' : 'AI_ASSISTED',
    storyboardCount: planForm.type === '原创' ? 1 : planForm.storyboardCount,
    trainingPolicy: 'NONE', platforms: planForm.platforms, deadline: toLocalDateTime(endDate, true)
  });
  if (publish) await publishContentPlan(planId, selectedEmployeeIds.value, `content-plan-${planId}-${Date.now()}`);
}
async function saveDraft() {
  if (!planForm.name || planForm.name.length > 30 || planForm.dateRange.length !== 2 || !releaseDateIsValid() || !taskStartIsValid() || !planForm.description || !platformQuotasAreValid()) {
    Message.warning('请填写计划名称、视频内容描述、平台视频条数，并选择大于当前时间3天的发布日期');
    return;
  }
  submitting.value = true;
  try { await persistPlan(false); Message.success('计划已保存为草稿'); createVisible.value = false; await loadPlans(); }
  catch (error) { Message.error(error instanceof Error ? error.message : '保存草稿失败'); }
  finally { submitting.value = false; }
}
async function submitPlan() {
  if (!validatePlan() || submitting.value) return;
  submitting.value = true;
  try {
    await persistPlan(true, Boolean(editingActivityId.value));
    Message.success(editingActivityId.value ? '计划修改成功' : `计划已提交，已创建 ${selectedEmployeeIds.value.length} 条员工任务`);
    createVisible.value = false;
    editingActivityId.value = undefined;
    await loadPlans();
  } catch (error) { Message.error(error instanceof Error ? error.message : '计划下发失败'); }
  finally { submitting.value = false; }
}
const selectedPlan = ref<PlanRow>();
const detailVisible = ref(false);
const deliveryVisible = ref(false);
function viewPlan(record: PlanRow) { selectedPlan.value = record; detailVisible.value = true; }
type DeliveryRow = {
  taskId: number;
  employeeNo: string;
  name: string;
  org: string;
  periodStart: string;
  periodEnd: string;
  status: string;
  statusColor: string;
};
const deliveryRows = ref<DeliveryRow[]>([]);
const deliveryLoading = ref(false);
const stagePresentation: Record<string, { label: string; color: string }> = {
  LOCKED: { label: '待训练', color: 'gray' },
  READY_TO_SHOOT: { label: '待拍摄', color: 'blue' },
  SHOOTING: { label: '拍摄中', color: 'arcoblue' },
  PROCESSING: { label: '处理中', color: 'purple' },
  PENDING_REVIEW: { label: '待审核', color: 'orange' },
  NEEDS_REVISION: { label: '待整改', color: 'red' },
  READY_TO_PUBLISH: { label: '待发布', color: 'cyan' },
  COMPLETED: { label: '已完成', color: 'green' },
  EXPIRED: { label: '已逾期', color: 'red' },
  TERMINATED: { label: '已终止', color: 'gray' }
};
function displayDateTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}
function toDeliveryRow(item: ContentDeliveryItem): DeliveryRow {
  const presentation = stagePresentation[item.stage] || { label: item.stage, color: 'gray' };
  return {
    taskId: item.taskId,
    employeeNo: item.employeeNumber || `UID${item.employeeId}`,
    name: item.employeeName || `员工${item.employeeId}`,
    org: [item.organizationName, item.storeName].filter(Boolean).join(' / ') || '未分配组织',
    periodStart: displayDateTime(item.createdTime),
    periodEnd: displayDateTime(item.deadline),
    status: presentation.label,
    statusColor: presentation.color
  };
}
async function viewDelivery(record: PlanRow) {
  selectedPlan.value = record;
  deliveryRows.value = [];
  deliveryVisible.value = true;
  deliveryLoading.value = true;
  try {
    deliveryRows.value = (await fetchContentDeliveryTasks(Number(record.id))).map(toDeliveryRow);
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '人员加载失败');
  } finally {
    deliveryLoading.value = false;
  }
}
function pausePlan(record: PlanRow) {
  Modal.warning({
    title: '暂停发布计划',
    content: `暂停后计划“${record.name}”将停止继续执行，是否继续？`,
    hideCancel: false,
    onOk: async () => {
      await pauseContentActivity(Number(record.id));
      Message.success('发布计划已暂停');
      await loadPlans();
    }
  });
}
function resumePlan(record: PlanRow) {
  Modal.confirm({
    title: '恢复发布计划',
    content: `恢复后计划“${record.name}”将继续执行，是否继续？`,
    hideCancel: false,
    onOk: async () => {
      await resumeContentActivity(Number(record.id));
      Message.success('发布计划已恢复');
      await loadPlans();
    }
  });
}
function terminatePlan(record: PlanRow) {
  Modal.warning({
    title: '终止发布计划',
    content: `终止后，计划“${record.name}”及未完成的员工任务将停止执行且无法恢复，是否继续？`,
    hideCancel: false,
    onOk: async () => {
      try {
        await terminateContentActivity(Number(record.id));
        await loadPlans();
        Message.success('计划已终止');
      } catch (error) {
        Message.error(error instanceof Error ? error.message : '计划终止失败');
        throw error;
      }
    }
  });
}
function deletePlan(record: PlanRow) {
  Modal.warning({
    title: '删除发布计划',
    content: `删除后计划“${record.name}”及其待执行员工任务将无法恢复，是否继续？`,
    hideCancel: false,
    onOk: async () => {
      await deleteContentActivity(Number(record.id));
      Message.success('发布计划已删除');
      await loadPlans();
    }
  });
}
const deliveryColumns: TableColumnData[] = [
  { title: '员工工号', dataIndex: 'employeeNo' }, { title: '发布人', dataIndex: 'name' }, { title: '组织', dataIndex: 'org', width: 240 },
  { title: '任务起始时间', slotName: 'period', width: 210 }, { title: '执行状态', slotName: 'status' }
];
const now = new Date();
const calendarYear = ref(now.getFullYear());
const calendarMonth = ref(now.getMonth() + 1);
const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
function toLocalIsoDate(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}
const calendarCells = computed(() => {
  const first = new Date(calendarYear.value, calendarMonth.value - 1, 1);
  const offset = (first.getDay() + 6) % 7;
  const daysInMonth = new Date(calendarYear.value, calendarMonth.value, 0).getDate();
  const cellCount = Math.ceil((offset + daysInMonth) / 7) * 7;
  return Array.from({ length: cellCount }, (_, index) => {
    const date = new Date(calendarYear.value, calendarMonth.value - 1, index - offset + 1);
    const current = date.getFullYear() === calendarYear.value && date.getMonth() === calendarMonth.value - 1;
    const day = date.getDate();
    const iso = toLocalIsoDate(date);
    const tasks = current
      ? allPlans.value.filter(plan => plan.rawStatus !== 'TERMINATED' && plan.startDate <= iso && plan.endDate >= iso)
      : [];
    return { key: iso, day, date: iso, current, today: iso === toLocalIsoDate(new Date()), tasks };
  });
});
function shiftMonth(offset: number) { const date = new Date(calendarYear.value, calendarMonth.value - 1 + offset, 1); calendarYear.value = date.getFullYear(); calendarMonth.value = date.getMonth() + 1; }
function goToday() { calendarYear.value = now.getFullYear(); calendarMonth.value = now.getMonth() + 1; }

const platforms = [
  { name: '全平台', short: '全', className: 'all' }, { name: '抖音', short: '抖', className: 'douyin' },
  { name: '快手', short: '快', className: 'kuaishou' }, { name: '小红书', short: '红', className: 'redbook' },
  { name: '视频号', short: '视', className: 'channels' }
];
const activePlatform = ref('全平台');
const analyticsFilters = reactive({ id: '', dateRange: [] as string[] });
const videoReports = ref<ContentVideoPerformanceItem[]>([]);
const videoReportsLoading = ref(false);
const reportSyncLabel: Record<string, string> = { PENDING: '待同步', SUCCESS: '已同步', FAILED: '同步失败' };
const filteredVideoReports = computed(() => videoReports.value.filter(item => {
  const date = item.publishTime.slice(0, 10);
  return (activePlatform.value === '全平台' || item.platform === activePlatform.value) &&
    (!analyticsFilters.id || String(item.taskId || item.platformVideoId).includes(analyticsFilters.id)) &&
    (!analyticsFilters.dateRange.length || (date >= analyticsFilters.dateRange[0] && date <= analyticsFilters.dateRange[1]));
}));
const analyticsMetrics = computed(() => {
  const rows = filteredVideoReports.value;
  const sum = (field: keyof ContentVideoPerformanceItem) => rows.reduce((total, item) => total + Number(item[field] || 0), 0);
  return [
    { label: '发布视频', value: rows.length.toLocaleString(), hint: `${rows.filter(item => item.syncStatus === 'SUCCESS').length} 条已同步`, tone: 'metric-blue' },
    { label: '总播放量', value: sum('viewCount').toLocaleString(), hint: '开放平台累计回传', tone: 'metric-purple' },
    { label: '总互动量', value: (sum('likeCount') + sum('commentCount') + sum('shareCount') + sum('favoriteCount')).toLocaleString(), hint: '点赞、评论、分享、收藏', tone: 'metric-green' }
  ];
});
const videoReportColumns: TableColumnData[] = [
  { title: '平台', dataIndex: 'platform', width: 90, fixed: 'left' },
  { title: '账号', dataIndex: 'accountName', width: 150, fixed: 'left' },
  { title: '视频', slotName: 'video', width: 240 },
  { title: '发布时间', dataIndex: 'publishTime', width: 170 },
  { title: '播放量', dataIndex: 'viewCount', width: 100 },
  { title: '点赞', dataIndex: 'likeCount', width: 90 },
  { title: '评论', dataIndex: 'commentCount', width: 90 },
  { title: '分享', dataIndex: 'shareCount', width: 90 },
  { title: '收藏', dataIndex: 'favoriteCount', width: 90 },
  { title: '涨粉', dataIndex: 'followerGain', width: 90 },
  { title: '转化数', dataIndex: 'conversionCount', width: 90 },
  { title: '成交额', slotName: 'amount', width: 120 },
  { title: '同步状态', slotName: 'syncStatus', width: 110 },
  { title: '最后同步', dataIndex: 'lastSyncTime', width: 170 }
];
async function loadVideoReports() {
  videoReportsLoading.value = true;
  try { videoReports.value = await fetchContentVideoPerformance(); }
  catch (error) { Message.error(error instanceof Error ? error.message : '视频报表加载失败'); }
  finally { videoReportsLoading.value = false; }
}
function resetAnalytics() { analyticsFilters.id = ''; analyticsFilters.dateRange = []; }

const accountPlatform = ref('全平台');
const bgmVideoTypes = ['产品种草', '门店探店', '品牌故事', '教程知识', '活动促销', '节日热点', '员工团队', '生活方式'];
const bgmMoods = ['轻松愉悦', '温暖治愈', '活力明快', '振奋激昂', '专业沉稳', '时尚酷感', '浪漫柔和', '悬念紧张'];
const bgmEnergyLevels = ['舒缓', '适中', '强节奏'];
const bgmVocalTypes = ['纯音乐', '女声', '男声', '混合人声'];
const bgmCopyrightStatuses = ['自有版权', '已获商用授权', '免版税/可商用', '版权待确认'];
const bgmFilters = reactive<{ keyword: string; videoType: string; mood: string; enabled?: boolean }>({ keyword: '', videoType: '', mood: '', enabled: undefined });
const bgms = ref<ContentBgmItem[]>([]);
const bgmLoading = ref(false);
const bgmEditorVisible = ref(false);
const bgmEditingId = ref<number>();
const bgmSaving = ref(false);
const bgmUploading = ref(false);
const bgmFormRef = ref<FormInstance>();
const emptyBgmForm = (folderId = 0): ContentBgmPayload => ({
  bgmName: '', fileUrl: '', originalFileName: '', folderId,
  videoType: '', mood: '', energyLevel: '适中',
  vocalType: '纯音乐', bpm: undefined, durationSeconds: undefined, copyrightStatus: '版权待确认',
  copyrightNote: '', enabled: true
});
const bgmForm = reactive<ContentBgmPayload>(emptyBgmForm());
const bgmColumns: TableColumnData[] = [
  { title: '素材名称 / 试听', slotName: 'audio', width: 300 },
  { title: '分类标签', slotName: 'classification', width: 280 },
  { title: '人声', dataIndex: 'vocalType', width: 100 },
  { title: 'BPM', dataIndex: 'bpm', width: 80 },
  { title: '时长', slotName: 'duration', width: 90 },
  { title: '版权信息', slotName: 'copyright', width: 260 },
  { title: '状态', slotName: 'enabled', width: 90 },
  { title: '更新时间', dataIndex: 'updateTime', width: 180 },
  { title: '操作', slotName: 'actions', width: 120, fixed: 'right' }
];
async function loadBgms() {
  bgmLoading.value = true;
  try { bgms.value = await fetchContentBgms({ ...bgmFilters }); }
  catch (error) { Message.error(error instanceof Error ? error.message : 'BGM加载失败'); }
  finally { bgmLoading.value = false; }
}
function resetBgmFilters() {
  Object.assign(bgmFilters, { keyword: '', videoType: '', mood: '', enabled: undefined });
  void loadBgms();
}
function openBgmEditor(record?: ContentBgmItem) {
  bgmEditingId.value = record?.id;
  Object.assign(bgmForm, record ? {
    bgmName: record.bgmName, fileUrl: record.fileUrl, originalFileName: record.originalFileName, folderId: record.folderId,
    videoType: record.videoType, mood: record.mood, energyLevel: record.energyLevel,
    vocalType: record.vocalType, bpm: record.bpm, durationSeconds: record.durationSeconds,
    copyrightStatus: record.copyrightStatus, copyrightNote: record.copyrightNote || '', enabled: record.enabled
  } : emptyBgmForm(currentMaterialFolderId.value === -1 ? 0 : Math.max(currentMaterialFolderId.value, 0)));
  bgmEditorVisible.value = true;
}
async function handleBgmFile(...args: unknown[]) {
  const file = getUploadFile(...args);
  if (!file) return;
  bgmUploading.value = true;
  try {
    const uploaded = await uploadContentBgm(file);
    bgmForm.fileUrl = uploaded.url;
    bgmForm.originalFileName = uploaded.originalName;
    if (!bgmForm.bgmName) bgmForm.bgmName = uploaded.originalName.replace(/\.[^.]+$/, '');
    Message.success('音频上传成功');
  } catch (error) { Message.error(error instanceof Error ? error.message : '音频上传失败'); }
  finally { bgmUploading.value = false; }
}
async function saveBgm() {
  const validation = await bgmFormRef.value?.validate();
  if (validation) return;
  if (!bgmForm.fileUrl) { Message.warning('请先上传音频文件'); return; }
  bgmSaving.value = true;
  try {
    if (bgmEditingId.value) await updateContentBgm(bgmEditingId.value, { ...bgmForm });
    else await createContentBgm({ ...bgmForm });
    Message.success(bgmEditingId.value ? 'BGM已更新' : 'BGM已添加');
    bgmEditorVisible.value = false;
    await loadBgms();
  } catch (error) { Message.error(error instanceof Error ? error.message : 'BGM保存失败'); }
  finally { bgmSaving.value = false; }
}
function removeBgm(record: ContentBgmItem) {
  Modal.warning({ title: '删除BGM', content: `确定删除“${record.bgmName}”吗？删除后列表不再显示。`, hideCancel: false, onOk: async () => {
    await deleteContentBgm(record.id); Message.success('BGM已删除'); await loadBgms();
  } });
}
function formatBgmDuration(seconds?: number) {
  if (!seconds) return '-';
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;
}

type MaterialTab = ContentMaterialType | 'AUDIO';
const activeMaterialTab = ref<MaterialTab>('VIDEO');
const materialTypes: Array<{ value: MaterialTab; label: string }> = [
  { value: 'VIDEO', label: '视频素材' },
  { value: 'AUDIO', label: '音频素材' },
  { value: 'IMAGE', label: '图片素材' }
];
const materialMeta: Record<MaterialTab, { label: string; accept: string; uploadHint: string; categories: string[] }> = {
  VIDEO: { label: '视频素材', accept: '.mp4,video/mp4', uploadHint: '支持 MP4，单个文件不超过 200MB', categories: ['产品展示', '门店环境', '人物出镜', '制作过程', '活动现场', '通用空镜'] },
  AUDIO: { label: '音频素材', accept: '.mp3,.wav,.m4a,audio/*', uploadHint: '支持 MP3、WAV、M4A，单个文件不超过 50MB', categories: bgmVideoTypes },
  IMAGE: { label: '图片素材', accept: '.jpg,.jpeg,.png,.webp,image/*', uploadHint: '支持 JPG、PNG、WEBP，单个文件不超过 10MB', categories: ['产品主图', '门店环境', '人物形象', '活动海报', '品牌元素', '视频封面'] }
};
const activeMaterialMeta = computed(() => materialMeta[activeMaterialTab.value]);
const materialFilters = reactive<{ keyword: string; category: string; enabled?: boolean }>({ keyword: '', category: '', enabled: undefined });
const materialsByType = reactive<Record<ContentMaterialType, ContentMaterialItem[]>>({ VIDEO: [], IMAGE: [] });
const materials = computed(() => {
  if (activeMaterialTab.value === 'AUDIO') return [];
  const list = materialsByType[activeMaterialTab.value];
  if (currentMaterialFolderId.value === 0) return list;
  if (currentMaterialFolderId.value === -1) return list.filter(m => m.folderId === 0);
  return list.filter(m => m.folderId === currentMaterialFolderId.value);
});
const materialLoading = ref(false);
const materialEditorVisible = ref(false);
const materialEditingId = ref<number>();
const materialSaving = ref(false);
const materialUploading = ref(false);
const materialFormRef = ref<FormInstance>();
const currentMaterialFolderId = ref<number>(0);
const materialFolders = ref<ContentMaterialFolderItem[]>([]);
const allMaterialFolders = ref<ContentMaterialFolderItem[]>([]);
const folderKeyword = ref('');
const folderLoading = ref(false);
const folderEditorVisible = ref(false);
const folderEditingId = ref<number>();
const folderSaving = ref(false);
const folderFormRef = ref<FormInstance>();
const moveVisible = ref(false);
const moving = ref(false);
const moveForm = reactive({ folderId: 0 });
const movingRecord = ref<{ id: number; type: MaterialTab }>();
const moveFolderOptions = computed(() => materialFolders.value.filter(folder => folder.id !== currentMaterialFolderId.value));
const folderForm = reactive<ContentMaterialFolderPayload>({
  folderName: '', materialType: 'VIDEO', parentId: 0, enabled: true, sortOrder: 0
});
const currentMaterialFolderName = computed(() => {
  if (currentMaterialFolderId.value === -1 || currentMaterialFolderId.value === 0) return '未分类';
  return materialFolders.value.find(f => f.id === currentMaterialFolderId.value)?.folderName || '文件夹';
});
const uncategorizedMaterialCount = computed(() => {
  if (activeMaterialTab.value === 'AUDIO') return bgms.value.filter(m => m.folderId === 0).length;
  return materialsByType[activeMaterialTab.value].filter(m => m.folderId === 0).length;
});
const emptyMaterialForm = (type: ContentMaterialType): ContentMaterialPayload => ({
  materialName: '', materialType: type, folderId: Math.max(currentMaterialFolderId.value, 0), fileUrl: '', originalFileName: '', fileSize: 0,
  category: '', tags: '', description: '', copyrightStatus: '版权待确认', copyrightNote: '', enabled: true
});
const materialForm = reactive<ContentMaterialPayload>(emptyMaterialForm('VIDEO'));
const activeBgms = computed(() => {
  if (currentMaterialFolderId.value === 0) return bgms.value;
  const folderId = currentMaterialFolderId.value === -1 ? 0 : currentMaterialFolderId.value;
  return bgms.value.filter(m => m.folderId === folderId);
});
const activeMaterialCount = computed(() => {
  if (activeMaterialTab.value === 'AUDIO') return activeBgms.value.length;
  return materials.value.length;
});
const materialColumns: TableColumnData[] = [
  { title: '素材名称 / 预览', slotName: 'preview', width: 360, fixed: 'left' },
  { title: '分类标签', slotName: 'classification', width: 260 },
  { title: '文件信息', slotName: 'fileInfo', width: 130 },
  { title: '版权信息', slotName: 'copyright', width: 240 },
  { title: '状态', slotName: 'enabled', width: 90 },
  { title: '更新时间', dataIndex: 'updateTime', width: 180 },
  { title: '操作', slotName: 'actions', width: 160, fixed: 'right' }
];
function materialCount(type: MaterialTab) {
  return type === 'AUDIO' ? bgms.value.length : materialsByType[type].length;
}
function uncategorizedCountFor(type: MaterialTab) {
  if (type === 'AUDIO') return bgms.value.filter(m => m.folderId === 0).length;
  return materialsByType[type].filter(m => m.folderId === 0).length;
}
type MaterialTreeNode = { key: string; title: string; children?: MaterialTreeNode[]; isLeaf?: boolean };
const materialTreeData = computed<MaterialTreeNode[]>(() =>
  materialTypes.map(t => {
    const rootFolders = allMaterialFolders.value.filter(f => f.materialType === t.value && f.parentId === 0);
    const children: MaterialTreeNode[] = [
      { key: `UNCAT:${t.value}`, title: `未分类 (${uncategorizedCountFor(t.value)})`, isLeaf: true },
      ...rootFolders.map(f => ({ key: `FOLDER:${f.id}`, title: f.folderName, isLeaf: true }))
    ];
    return { key: `TYPE:${t.value}`, title: `${t.label} (${materialCount(t.value)})`, children };
  })
);
const materialTreeSelectedKeys = computed<Array<string | number>>(() => {
  if (currentMaterialFolderId.value === 0) return [`TYPE:${activeMaterialTab.value}`];
  if (currentMaterialFolderId.value === -1) return [`UNCAT:${activeMaterialTab.value}`];
  return [`FOLDER:${currentMaterialFolderId.value}`];
});
function onMaterialTreeSelect(keys: Array<string | number>) {
  const key = String(keys[0] ?? '');
  if (!key) return;
  let targetType: MaterialTab | undefined;
  let targetFolderId = 0;
  if (key.startsWith('TYPE:')) {
    targetType = key.slice(5) as MaterialTab;
    targetFolderId = 0;
  } else if (key.startsWith('UNCAT:')) {
    targetType = key.slice(6) as MaterialTab;
    targetFolderId = -1;
  } else if (key.startsWith('FOLDER:')) {
    const folderId = Number(key.slice(7));
    const folder = allMaterialFolders.value.find(f => f.id === folderId);
    targetType = (folder?.materialType as MaterialTab) ?? activeMaterialTab.value;
    targetFolderId = folderId;
  }
  if (!targetType) return;
  if (targetType !== activeMaterialTab.value) {
    activeMaterialTab.value = targetType; // watch 会在下一个 tick 重置为根视图并加载
    void nextTick(() => enterMaterialFolder(targetFolderId));
  } else {
    enterMaterialFolder(targetFolderId);
  }
}
async function loadMaterialFolders(type: ContentMaterialFolderType) {
  folderLoading.value = true;
  try {
    allMaterialFolders.value = await fetchContentMaterialFolders(type, { keyword: folderKeyword.value.trim() || undefined });
    materialFolders.value = allMaterialFolders.value.filter(f => f.parentId === 0);
  }
  catch (error) { Message.error(error instanceof Error ? error.message : '文件夹加载失败'); }
  finally { folderLoading.value = false; }
}
function reloadMaterialFolders() {
  void loadMaterialFolders(activeMaterialTab.value as ContentMaterialFolderType);
}
async function loadMaterialType(type: ContentMaterialType) {
  if (currentMaterialFolderId.value === 0) await loadMaterialFolders(type);
  materialLoading.value = true;
  try {
    const folderId = currentMaterialFolderId.value === 0 ? undefined
      : (currentMaterialFolderId.value === -1 ? 0 : currentMaterialFolderId.value);
    materialsByType[type] = await fetchContentMaterials({ materialType: type, folderId, ...materialFilters });
  }
  catch (error) { Message.error(error instanceof Error ? error.message : '素材加载失败'); }
  finally { materialLoading.value = false; }
}
async function loadActiveMaterials() {
  if (activeMaterialTab.value === 'AUDIO') {
    if (currentMaterialFolderId.value === 0) await loadMaterialFolders('AUDIO');
    await loadBgms();
  }
  else await loadMaterialType(activeMaterialTab.value);
}
function enterMaterialFolder(folderId: number) {
  currentMaterialFolderId.value = folderId;
  Object.assign(materialFilters, { keyword: '', category: '', enabled: undefined });
  void loadActiveMaterials();
}
function leaveMaterialFolder() {
  currentMaterialFolderId.value = 0;
  Object.assign(materialFilters, { keyword: '', category: '', enabled: undefined });
  void loadActiveMaterials();
}
function resetActiveMaterialFilters() {
  if (activeMaterialTab.value === 'AUDIO') resetBgmFilters();
  else {
    Object.assign(materialFilters, { keyword: '', category: '', enabled: undefined });
    void loadMaterialType(activeMaterialTab.value as ContentMaterialType);
  }
}
function openActiveMaterialEditor() {
  if (activeMaterialTab.value === 'AUDIO') openBgmEditor();
  else openMaterialEditor();
}
function openMaterialEditor(record?: ContentMaterialItem) {
  if (activeMaterialTab.value === 'AUDIO') return;
  materialEditingId.value = record?.id;
  Object.assign(materialForm, record ? {
    materialName: record.materialName, materialType: record.materialType, folderId: record.folderId, fileUrl: record.fileUrl,
    originalFileName: record.originalFileName, fileSize: record.fileSize, category: record.category,
    tags: record.tags || '', description: record.description || '', copyrightStatus: record.copyrightStatus,
    copyrightNote: record.copyrightNote || '', enabled: record.enabled
  } : emptyMaterialForm(activeMaterialTab.value));
  materialEditorVisible.value = true;
}
function openMaterialFolderEditor(record?: ContentMaterialFolderItem) {
  folderEditingId.value = record?.id;
  Object.assign(folderForm, record ? {
    folderName: record.folderName, materialType: record.materialType, parentId: record.parentId,
    enabled: record.enabled, sortOrder: record.sortOrder
  } : { folderName: '', materialType: activeMaterialTab.value as ContentMaterialFolderType, parentId: 0, enabled: true, sortOrder: 0 });
  folderEditorVisible.value = true;
}
async function toggleMaterialFolder(record: ContentMaterialFolderItem) {
  try {
    await updateContentMaterialFolder(record.id, {
      folderName: record.folderName, materialType: record.materialType, parentId: record.parentId,
      enabled: !record.enabled, sortOrder: record.sortOrder
    });
    Message.success(record.enabled ? '文件夹已停用' : '文件夹已启用');
    await loadMaterialFolders(activeMaterialTab.value as ContentMaterialFolderType);
  } catch (error) { Message.error(error instanceof Error ? error.message : '文件夹状态更新失败'); }
}
function handleFolderAction(folder: ContentMaterialFolderItem, action: string) {
  if (action === 'toggle') void toggleMaterialFolder(folder);
  else if (action === 'rename') openMaterialFolderEditor(folder);
  else if (action === 'delete') removeMaterialFolder(folder);
}
function openMoveMaterial(record: ContentMaterialItem | ContentBgmItem) {
  movingRecord.value = { id: record.id, type: activeMaterialTab.value };
  moveForm.folderId = currentMaterialFolderId.value === -1 ? 0 : currentMaterialFolderId.value;
  moveVisible.value = true;
}
async function confirmMoveMaterial() {
  const record = movingRecord.value;
  if (!record) return;
  moving.value = true;
  try {
    if (record.type === 'AUDIO') await moveContentBgm(record.id, moveForm.folderId);
    else await moveContentMaterial(record.id, moveForm.folderId);
    Message.success('素材已移动');
    moveVisible.value = false;
    await loadActiveMaterials();
  } catch (error) { Message.error(error instanceof Error ? error.message : '移动失败'); }
  finally { moving.value = false; }
}
async function saveMaterialFolder() {
  const validation = await folderFormRef.value?.validate();
  if (validation) return;
  folderSaving.value = true;
  try {
    const payload = { ...folderForm };
    if (folderEditingId.value) await updateContentMaterialFolder(folderEditingId.value, payload);
    else await createContentMaterialFolder(payload);
    Message.success(folderEditingId.value ? '文件夹已更新' : '文件夹已创建');
    folderEditorVisible.value = false;
    await loadMaterialFolders(activeMaterialTab.value as ContentMaterialFolderType);
  } catch (error) { Message.error(error instanceof Error ? error.message : '文件夹保存失败'); }
  finally { folderSaving.value = false; }
}
function removeMaterialFolder(record: ContentMaterialFolderItem) {
  if (record.itemCount > 0) { Message.warning('文件夹内还有素材，请先清空后再删除'); return; }
  Modal.warning({ title: '删除文件夹', content: `确定删除文件夹“${record.folderName}”吗？删除后不可恢复。`, hideCancel: false, onOk: async () => {
    await deleteContentMaterialFolder(record.id); Message.success('文件夹已删除'); await loadMaterialFolders(activeMaterialTab.value as ContentMaterialFolderType);
  } });
}
async function handleMaterialFile(...args: unknown[]) {
  const file = getUploadFile(...args);
  if (!file || activeMaterialTab.value === 'AUDIO') return;
  materialUploading.value = true;
  try {
    const uploaded = await uploadContentMaterial(activeMaterialTab.value, file);
    materialForm.fileUrl = uploaded.url;
    materialForm.originalFileName = uploaded.originalName;
    materialForm.fileSize = uploaded.size;
    if (!materialForm.materialName) materialForm.materialName = uploaded.originalName.replace(/\.[^.]+$/, '');
    Message.success('素材文件上传成功');
  } catch (error) { Message.error(error instanceof Error ? error.message : '素材文件上传失败'); }
  finally { materialUploading.value = false; }
}
async function saveMaterial() {
  const validation = await materialFormRef.value?.validate();
  if (validation) return;
  if (!materialForm.fileUrl) { Message.warning('请先上传素材文件'); return; }
  materialSaving.value = true;
  try {
    if (materialEditingId.value) await updateContentMaterial(materialEditingId.value, { ...materialForm });
    else await createContentMaterial({ ...materialForm });
    Message.success(materialEditingId.value ? '素材已更新' : '素材已添加');
    materialEditorVisible.value = false;
    await loadMaterialType(materialForm.materialType);
  } catch (error) { Message.error(error instanceof Error ? error.message : '素材保存失败'); }
  finally { materialSaving.value = false; }
}
function removeMaterial(record: ContentMaterialItem) {
  Modal.warning({ title: '删除素材', content: `确定删除“${record.materialName}”吗？删除后列表不再显示。`, hideCancel: false, onOk: async () => {
    await deleteContentMaterial(record.id); Message.success('素材已删除'); await loadMaterialType(record.materialType);
  } });
}
function splitMaterialTags(tags?: string) { return (tags || '').split(/[,，]/).map(item => item.trim()).filter(Boolean).slice(0, 4); }
function materialExtension(fileName: string) { return (fileName.split('.').pop() || '-').toUpperCase(); }
function formatFileSize(bytes?: number) {
  if (!bytes) return '-';
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}
function formatDate(value?: string) {
  if (!value) return '-';
  return value.replace('T', ' ').substring(0, 16);
}

watch(activeMaterialTab, () => {
  currentMaterialFolderId.value = 0;
  Object.assign(materialFilters, { keyword: '', category: '', enabled: undefined });
  void loadActiveMaterials();
});

const accountFilters = reactive({ keyword: '', type: '', status: '', employee: '' });
type AccountRow = {
  id: number; index: number; platform: string; platformShort: string; platformClass: string;
  name: string; accountId: string; homepageUrl?: string; type: string; org: string; employee: string;
  employeeId: number; organizationId?: number; updated: string; status: string; rawStatus: ContentAccountItem['status'];
};
const accountColumns: TableColumnData[] = [
  { title: '序号', dataIndex: 'index', width: 80 }, { title: '平台', slotName: 'platform', width: 130 },
  { title: '账号名称', dataIndex: 'name', width: 180 }, { title: '平台账号ID', dataIndex: 'accountId', width: 180 },
  { title: '账号类型', dataIndex: 'type' }, { title: '归属组织', dataIndex: 'org', width: 220 },
  { title: '归属员工', dataIndex: 'employee' }, { title: '更新时间', dataIndex: 'updated', width: 170 },
  { title: '账号状态', slotName: 'status' }, { title: '操作', slotName: 'actions', width: 260, fixed: 'right' }
];
const planAccountColumns: TableColumnData[] = [
  { title: '发布平台', dataIndex: 'platform', width: 110 },
  { title: '账号名称', dataIndex: 'name', width: 180 },
  { title: '平台账号ID', dataIndex: 'accountId', width: 180 },
  { title: '账号类型', dataIndex: 'type', width: 100 },
  { title: '归属组织', dataIndex: 'org', width: 180 },
  { title: '归属员工', dataIndex: 'employee', width: 160 },
  { title: '账号状态', slotName: 'status', width: 110 }
];
const accounts = ref<AccountRow[]>([]);
const accountsLoading = ref(false);
const selectedAccountIds = ref<number[]>([]);
const planAccountVisible = ref(false);
const planAccountKeyword = ref('');
const selectedPlanAccountIds = ref<number[]>([]);
const accountStatusLabel: Record<string, string> = {
  ACTIVE: '正常', PENDING: '待校验', FAILED: '校验失败', AUTH_EXPIRED: '授权失效', DISABLED: '停用'
};
const accountPlatformMeta: Record<string, { short: string; className: string }> = {
  抖音: { short: '抖', className: 'douyin' }, 快手: { short: '快', className: 'kuaishou' },
  小红书: { short: '红', className: 'redbook' }, 视频号: { short: '视', className: 'channels' }
};
function toAccountRow(item: ContentAccountItem, index: number): AccountRow {
  const platformMeta = accountPlatformMeta[item.platform] || { short: item.platform.slice(0, 1), className: 'all' };
  return {
    id: item.id, index: index + 1, platform: item.platform, platformShort: platformMeta.short,
    platformClass: platformMeta.className, name: item.accountName, accountId: item.platformAccountId, homepageUrl: item.platformHomepageUrl,
    type: item.accountType, org: item.organizationName || '未分配组织',
    employee: `${item.employeeName}${item.employeeNumber ? ` / ${item.employeeNumber}` : ''}`,
    employeeId: item.employeeId, organizationId: item.organizationId,
    updated: item.updateTime.replace('T', ' ').slice(0, 16),
    status: accountStatusLabel[item.status] || item.status, rawStatus: item.status
  };
}
async function loadAccounts(rethrow = false) {
  accountsLoading.value = true;
  try { accounts.value = (await fetchContentAccounts()).map(toAccountRow); }
  catch (error) {
    Message.error(error instanceof Error ? error.message : '账号加载失败');
    if (rethrow) throw error;
  }
  finally { accountsLoading.value = false; }
}
const filteredAccounts = computed(() => accounts.value.filter(item =>
  (accountPlatform.value === '全平台' || item.platform === accountPlatform.value) &&
  (!accountFilters.keyword || item.name.includes(accountFilters.keyword) || item.accountId.includes(accountFilters.keyword)) &&
  (!accountFilters.type || item.type === accountFilters.type) &&
  (!accountFilters.status || item.status === accountFilters.status) &&
  (!accountFilters.employee || item.employee.includes(accountFilters.employee))
));
const selectablePlanAccounts = computed(() => {
  const keyword = planAccountKeyword.value.trim().toLowerCase();
  if (!keyword) return accounts.value;
  return accounts.value.filter(account =>
    `${account.name} ${account.accountId} ${account.employee}`.toLowerCase().includes(keyword)
  );
});
async function openPlanAccountSelector() {
  planAccountKeyword.value = '';
  planAccountVisible.value = true;
  await loadAccounts();
}
function confirmPlanAccounts() {
  const selectedAccounts = accounts.value.filter(account => selectedPlanAccountIds.value.includes(account.id));
  selectedEmployeeIds.value = [...new Set(selectedAccounts.map(account => account.employeeId))];
  planForm.employeeCount = selectedEmployeeIds.value.length;
  planAccountVisible.value = false;
}
function resetAccounts() { Object.assign(accountFilters, { keyword: '', type: '', status: '', employee: '' }); }
function downloadCsv(filename: string, rows: unknown[][]) {
  const csv = `\uFEFF${rows.map(row => row.map(value => `"${String(value ?? '').replaceAll('"', '""')}"`).join(',')).join('\r\n')}`;
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  const anchor = document.createElement('a'); anchor.href = url; anchor.download = filename; anchor.click(); URL.revokeObjectURL(url);
}
function exportAccounts() {
  downloadCsv('账号.csv', [
    ['平台', '账号名称', '平台账号ID', '账号类型', '归属组织', '归属员工', '账号状态', '更新时间'],
    ...filteredAccounts.value.map(item => [item.platform, item.name, item.accountId, item.type, item.org, item.employee, item.status, item.updated])
  ]);
}
const accountEditorVisible = ref(false);
const accountEditingId = ref<number>();
const accountReadonly = ref(false);
const accountFormRef = ref<FormInstance>();
const accountRules = {
  platform: [{ required: true, message: '请选择发布平台' }],
  accountType: [{ required: true, message: '请选择账号类型' }],
  accountName: [{ required: true, message: '请输入账号名称' }, { minLength: 2, message: '账号名称至少2个字符' }],
  platformAccountId: [
    { required: true, message: '请输入平台账号ID' },
    { match: /^[A-Za-z0-9_.@-]+$/, message: '平台账号ID仅支持字母、数字及 . _ - @' }
  ],
  platformHomepageUrl: [{ match: /^https?:\/\/[^\s]+$/i, message: '请输入以 http:// 或 https:// 开头的有效地址' }],
  employeeId: [{ required: true, type: 'number' as const, min: 1, message: '请选择归属员工' }]
};
type AccountFormState = Omit<ContentAccountPayload, 'employeeId'> & { employeeId?: number };
const accountForm = reactive<AccountFormState>({ platform: '抖音', accountName: '', platformAccountId: '', platformHomepageUrl: '', accountType: '职人', employeeId: undefined });
async function openAccountEditor(record?: AccountRow, readonly = false) {
  await loadEmployees('account');
  accountEditingId.value = record?.id;
  accountReadonly.value = readonly;
  Object.assign(accountForm, record ? {
    platform: record.platform, accountName: record.name, platformAccountId: record.accountId, platformHomepageUrl: record.homepageUrl || '',
    accountType: record.type, organizationId: record.organizationId,
    employeeId: record.employeeId
  } : { platform: '抖音', accountName: '', platformAccountId: '', platformHomepageUrl: '', accountType: '职人', organizationId: undefined, employeeId: undefined });
  accountFormRef.value?.clearValidate?.();
  accountEditorVisible.value = true;
}
function syncAccountOrganization(value: unknown) {
  const employeeId = Number(value);
  accountForm.organizationId = employees.value.find(item => item.id === employeeId)?.organizationId;
}
async function saveAccount() {
  if (accountReadonly.value) { accountEditorVisible.value = false; return; }
  const errors = await accountFormRef.value?.validate?.();
  if (errors) return;
  try {
    const payload = { ...accountForm, employeeId: accountForm.employeeId! };
    let savedId = accountEditingId.value;
    if (savedId) await updateContentAccount(savedId, payload);
    else savedId = await createContentAccount(payload);
    const employee = employees.value.find(item => item.id === accountForm.employeeId);
    const optimisticItem: ContentAccountItem = {
      id: savedId,
      platform: accountForm.platform,
      accountName: accountForm.accountName,
      platformAccountId: accountForm.platformAccountId,
      platformHomepageUrl: accountForm.platformHomepageUrl,
      accountType: accountForm.accountType,
      organizationId: accountForm.organizationId,
      organizationName: employee?.organizationName,
      employeeId: accountForm.employeeId!,
      employeeName: employee?.displayName || '',
      employeeNumber: employee?.employeeNumber,
      status: accountEditingId.value
        ? accounts.value.find(item => item.id === savedId)?.rawStatus || 'PENDING'
        : 'PENDING',
      updateTime: new Date().toISOString()
    };
    const nextRows = accounts.value.filter(item => item.id !== savedId);
    accounts.value = [toAccountRow(optimisticItem, 0), ...nextRows].map((item, index) => ({ ...item, index: index + 1 }));
    Message.success(accountEditingId.value ? '账号已更新' : '账号已新增');
    accountEditorVisible.value = false;
    try { await loadAccounts(true); }
    catch { Message.warning('账号已保存，但列表同步失败；已保留当前记录，请稍后刷新'); }
  } catch (error) { Message.error(error instanceof Error ? error.message : '账号保存失败'); }
}
function removeAccounts(ids: number[]) {
  Modal.warning({ title: ids.length > 1 ? '批量删除账号' : '删除账号', content: `确定删除选中的 ${ids.length} 个账号吗？`, hideCancel: false, onOk: async () => {
    await deleteContentAccounts(ids); selectedAccountIds.value = []; Message.success('账号已删除'); await loadAccounts();
  } });
}
function viewPlatformHomepage(record: AccountRow) {
  if (!record.homepageUrl) {
    Message.warning('该账号暂未维护平台主页地址，请先编辑账号补充');
    return;
  }
  window.open(record.homepageUrl, '_blank', 'noopener,noreferrer');
}
const importVisible = ref(false);
const accountImporting = ref(false);
const importFile = ref<File>();
async function openImport() { await loadEmployees('account'); importFile.value = undefined; importVisible.value = true; }
function downloadAccountTemplate() { downloadCsv('账号导入模板.csv', [['平台', '账号名称', '平台账号ID', '账号类型', '员工工号'], ['抖音', '示例账号', 'DY000001', '职人', '100001']]); }
function handleImportFile(files: UploadFileEntry[]) { importFile.value = getUploadFile(files); }
async function finishImport() {
  if (!importFile.value) { Message.warning('请选择CSV文件'); return; }
  accountImporting.value = true;
  try {
    const lines = (await importFile.value.text()).replace(/^\uFEFF/, '').split(/\r?\n/).filter(Boolean);
    const records = lines.slice(1).map((line, index) => {
      const [platform, accountName, platformAccountId, accountType, employeeNumber] = line.split(',').map(value => value.replace(/^"|"$/g, '').trim());
      if (!platform || !accountName || !platformAccountId || !accountType || !employeeNumber) throw new Error(`第 ${index + 2} 行存在空的必填字段`);
      if (!['抖音', '快手', '小红书', '视频号'].includes(platform)) throw new Error(`第 ${index + 2} 行发布平台不正确`);
      if (!['蓝V', '职人', '个人'].includes(accountType)) throw new Error(`第 ${index + 2} 行账号类型不正确`);
      if (!/^[A-Za-z0-9_.@-]+$/.test(platformAccountId)) throw new Error(`第 ${index + 2} 行平台账号ID格式不正确`);
      const employee = employees.value.find(item => (item.employeeNumber || item.username) === employeeNumber);
      if (!employee) throw new Error(`第 ${index + 2} 行员工工号不存在或不可下发`);
      return { platform, accountName, platformAccountId, accountType, employeeId: employee.id, organizationId: employee.organizationId };
    });
    const count = await importContentAccounts(records);
    importVisible.value = false; Message.success(`成功导入 ${count} 个账号`); await loadAccounts();
  } catch (error) { Message.error(error instanceof Error ? error.message : '账号导入失败'); }
  finally { accountImporting.value = false; }
}

const employeeVisible = ref(false);
const employeesLoading = ref(false);
const employees = ref<UserItem[]>([]);
const organizations = ref<OrganizationOption[]>([]);
const employeeKeyword = ref('');
const selectedEmployeeIds = ref<number[]>([]);
const personnelImportVisible = ref(false);
const personnelImportFile = ref<File>();
const personnelValidating = ref(false);
const personnelImportRows = ref<PersonnelImportResult[]>([]);
const selectedPersonnelVisible = ref(false);
const importedPersonnelDetails = ref<Record<number, PersonnelImportResult>>({});
const validPersonnelRows = computed(() => personnelImportRows.value.filter(row => row.status === 'VALID' && row.userId));
const personnelImportColumns: TableColumnData[] = [
  { title: '行号', dataIndex: 'rowNumber', width: 70 },
  { title: '组织（门店）', dataIndex: 'organizationStore', width: 180 },
  { title: '姓名', dataIndex: 'name', width: 100 },
  { title: '手机号', dataIndex: 'phone', width: 130 },
  { title: '部门', dataIndex: 'department', width: 180 },
  { title: '岗位', dataIndex: 'position', width: 130 },
  { title: '校验结果', slotName: 'validation', width: 170 }
];
const selectedPersonnelColumns: TableColumnData[] = [
  { title: '组织（门店）', dataIndex: 'organizationStore', width: 210 },
  { title: '姓名', dataIndex: 'name', width: 120 },
  { title: '手机号', dataIndex: 'phone', width: 150 },
  { title: '部门', dataIndex: 'department', width: 180 },
  { title: '岗位', dataIndex: 'position', width: 150 }
];
type EmployeeTreeNode = {
  key: string | number;
  title: string;
  isEmployee?: boolean;
  employeeNumber?: string;
  employeeCount?: number;
  disableCheckbox?: boolean;
  children?: EmployeeTreeNode[];
};
function organizationNode(organization: OrganizationOption): EmployeeTreeNode {
  const employeeChildren: EmployeeTreeNode[] = employees.value
    .filter(employee => employee.organizationId === organization.id)
    .map(employee => ({
      key: employee.id,
      title: employee.displayName,
      employeeNumber: employee.employeeNumber || employee.username || '',
      isEmployee: true
    }));
  const organizationChildren = (organization.children || []).map(organizationNode);
  const children = [...organizationChildren, ...employeeChildren];
  return {
    key: `org-${organization.id}`,
    title: organization.name,
    employeeCount: children.reduce((total, child) => total + (child.isEmployee ? 1 : child.employeeCount || 0), 0),
    disableCheckbox: true,
    children
  };
}
function filterTree(nodes: EmployeeTreeNode[], keyword: string): EmployeeTreeNode[] {
  if (!keyword) return nodes;
  return nodes.flatMap(node => {
    const selfMatches = `${node.title} ${node.employeeNumber || ''}`.toLowerCase().includes(keyword);
    if (selfMatches) return [node];
    const children = filterTree(node.children || [], keyword);
    return children.length ? [{ ...node, children }] : [];
  });
}
function buildOrganizationTree(records: OrganizationOption[]): OrganizationOption[] {
  const nodeMap = new Map<number, OrganizationOption>(
    records.map(record => [record.id, { ...record, children: [] }])
  );
  const roots: OrganizationOption[] = [];
  nodeMap.forEach(node => {
    const parent = node.parentId ? nodeMap.get(node.parentId) : undefined;
    if (parent) parent.children?.push(node);
    else roots.push(node);
  });
  return roots;
}
const employeeTreeData = computed(() => {
  const assignedIds = new Set<number>();
  const roots = buildOrganizationTree(organizations.value).map(organization => {
    const collect = (node: OrganizationOption) => {
      employees.value.filter(employee => employee.organizationId === node.id).forEach(employee => assignedIds.add(employee.id));
      (node.children || []).forEach(collect);
    };
    collect(organization);
    return organizationNode(organization);
  });
  const unassigned: EmployeeTreeNode[] = employees.value
    .filter(employee => !assignedIds.has(employee.id))
    .map(employee => ({
      key: employee.id,
      title: employee.displayName,
      employeeNumber: employee.employeeNumber || employee.username || '',
      isEmployee: true
    }));
  if (unassigned.length) {
    roots.push({
      key: 'org-unassigned',
      title: '未分配组织',
      employeeCount: unassigned.length,
      disableCheckbox: true,
      children: unassigned
    });
  }
  return filterTree(roots, employeeKeyword.value.trim().toLowerCase());
});
const selectedEmployees = computed(() =>
  selectedEmployeeIds.value
    .map(id => employees.value.find(employee => employee.id === id))
    .filter((employee): employee is UserItem => Boolean(employee))
);
const selectedPersonnelRows = computed(() => selectedEmployeeIds.value.map(id => {
  const imported = importedPersonnelDetails.value[id];
  if (imported) {
    return {
      id,
      organizationStore: imported.organizationStore || '-',
      name: imported.name || imported.inputName || '-',
      phone: imported.phone || imported.inputPhone || '-',
      department: imported.department || '-',
      position: imported.position || '-'
    };
  }
  const employee = employees.value.find(item => item.id === id);
  return {
    id,
    organizationStore: employee?.primaryStoreName || '-',
    name: employee?.displayName || '-',
    phone: employee?.phone || '-',
    department: employee?.department || employee?.organizationName || '-',
    position: employee?.position || employee?.roleNames?.join('、') || '-'
  };
}));
async function loadEmployees(scope: 'plan' | 'account' = 'plan') {
  if (employees.value.length || employeesLoading.value) return;
  employeesLoading.value = true;
  try {
    const fetchUsers = scope === 'account' ? fetchContentAccountUsers : fetchContentTaskUsers;
    const fetchOrganizations = scope === 'account'
      ? fetchContentAccountOrganizations
      : fetchContentTaskOrganizations;
    const [firstPage, organizationRecords] = await Promise.all([
      fetchUsers(1, 100),
      fetchOrganizations()
    ]);
    const pageCount = Math.ceil(firstPage.total / firstPage.pageSize);
    const remainingPages = pageCount > 1
      ? await Promise.all(Array.from({ length: pageCount - 1 }, (_, index) => fetchUsers(index + 2, 100)))
      : [];
    employees.value = [firstPage, ...remainingPages]
      .flatMap(page => page.records)
      .filter(item => item.loginEnabled);
    organizations.value = organizationRecords;
  } catch (error) { Message.error(error instanceof Error ? error.message : '员工列表加载失败'); }
  finally { employeesLoading.value = false; }
}
function removeEmployee(id: number) {
  selectedEmployeeIds.value = selectedEmployeeIds.value.filter(employeeId => employeeId !== id);
}
function openEmployeeSelector() {
  employeeKeyword.value = '';
  employeeVisible.value = true;
}
function openPersonnelImport() {
  personnelImportFile.value = undefined;
  personnelImportRows.value = [];
  personnelImportVisible.value = true;
}
function handlePersonnelFile(files: UploadFileEntry[]) {
  personnelImportFile.value = getUploadFile(files);
  personnelImportRows.value = [];
}
async function downloadPersonnelTemplate() {
  try {
    const blob = await downloadPersonnelImportTemplate();
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = '人员导入模板.xlsx';
    anchor.click();
    URL.revokeObjectURL(url);
  } catch (error) { Message.error(error instanceof Error ? error.message : '模板下载失败'); }
}
async function validatePersonnelFile() {
  if (!personnelImportFile.value) { Message.warning('请先选择Excel文件'); return; }
  personnelValidating.value = true;
  try {
    personnelImportRows.value = await validatePersonnelImport(personnelImportFile.value);
    const validCount = validPersonnelRows.value.length;
    if (validCount) Message.success(`校验完成，${validCount} 人通过`);
    else Message.warning('校验完成，没有可添加的人员');
  } catch (error) { Message.error(error instanceof Error ? error.message : '人员校验失败'); }
  finally { personnelValidating.value = false; }
}
function applyImportedPersonnel() {
  importedPersonnelDetails.value = {
    ...importedPersonnelDetails.value,
    ...Object.fromEntries(validPersonnelRows.value.map(row => [row.userId!, row]))
  };
  selectedEmployeeIds.value = [...new Set([
    ...selectedEmployeeIds.value,
    ...validPersonnelRows.value.map(row => row.userId!)
  ])];
  planForm.employeeCount = selectedEmployeeIds.value.length;
  personnelImportVisible.value = false;
  Message.success(`已添加 ${validPersonnelRows.value.length} 名校验通过人员`);
}
function confirmEmployees() { planForm.employeeCount = selectedEmployeeIds.value.length; employeeVisible.value = false; }

watch(
  activeModule,
  async (module) => {
    createVisible.value = false;
    if (module === 'accounts') await loadAccounts();
    else if (module === 'bgm') await Promise.all([loadBgms(), loadMaterialType('VIDEO'), loadMaterialType('IMAGE')]);
    else if (module === 'analytics') await loadVideoReports();
    else await loadPlans();
  },
  { immediate: true }
);
onMounted(() => {
  statusClock.value = Date.now();
  statusClockTimer = setInterval(() => { statusClock.value = Date.now(); }, 1000);
});
onUnmounted(() => {
  if (statusClockTimer) clearInterval(statusClockTimer);
});
</script>

<style scoped>
.video-center { min-width:1080px; padding:16px 20px 24px; box-sizing:border-box; color:var(--tql-text-primary, var(--tql-text-primary)); }
.module-content { display:grid; min-width:0; gap:16px; }
.search-panel, .data-panel, .calendar-panel, .trend-panel { width:100%; min-width:0; box-sizing:border-box; overflow:hidden; border:1px solid var(--tql-border); border-radius:8px; box-shadow:none; }
.search-panel :deep(.arco-card-body) { display:flex; align-items:flex-start; gap:20px; padding:20px 24px; }
.search-panel form { display:grid; min-width:0; width:0; grid-template-columns:repeat(3, minmax(220px, 1fr)); gap:16px 24px; flex:1; }
.search-panel.compact form { display:flex; }
.search-panel :deep(.arco-form-item) { margin:0; }
.search-panel :deep(.arco-form-item-label-col) { min-width:70px; }
.search-panel .plan-search-form {
  display:flex;
  flex:1 1 auto;
  width:auto;
  min-width:0;
  flex-wrap:wrap;
  align-content:flex-start;
  justify-content:flex-start;
  gap:16px 24px;
}
.search-panel .plan-search-form :deep(.plan-search-item) {
  flex:0 0 300px;
  width:300px;
  min-width:300px;
  max-width:300px;
}
.search-panel .plan-search-form :deep(.plan-date-item) {
  flex-basis:390px;
  width:390px;
  min-width:390px;
  max-width:390px;
}
.search-panel .plan-search-form :deep(.plan-query-control) {
  flex:0 0 230px;
  width:230px;
  min-width:230px;
  max-width:230px;
}
.search-panel .plan-search-form :deep(.plan-date-item .arco-picker) { width:320px; }
.search-panel > :deep(.arco-card-body) > .search-actions {
  align-self:stretch;
  margin-left:auto;
}
.search-actions { display:flex; flex:0 0 auto; gap:8px; padding-left:16px; border-left:1px solid var(--tql-border); }
.panel-toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px; }
.panel-toolbar > div { display:flex; align-items:center; gap:10px; }
.panel-toolbar strong { font-size:16px; }
.panel-toolbar span { color:var(--tql-text-tertiary); font-size:12px; }
.status-dot { display:inline-flex; align-items:center; gap:7px; white-space:nowrap; }
.status-dot::before { width:7px; height:7px; border-radius:50%; content:""; }
.status-active::before { background:var(--tql-success); box-shadow:0 0 0 3px var(--tql-success-soft); }
.status-draft::before { background:var(--tql-warning); box-shadow:0 0 0 3px var(--tql-warning-soft); }
.status-terminated::before { background:var(--tql-danger); box-shadow:0 0 0 3px var(--tql-border-danger-soft); }
.status-done::before { background:var(--tql-text-tertiary); box-shadow:0 0 0 3px var(--tql-bg-hover); }
.progress-cell { display:flex; align-items:center; gap:10px; }
.progress-cell :deep(.arco-progress) { width:105px; }
.progress-cell span { color:var(--tql-text-secondary); font-size:12px; }
small { color:var(--tql-text-tertiary); }
.empty-state { display:flex; min-height:320px; flex-direction:column; align-items:center; justify-content:center; }
.empty-illustration { display:grid; width:62px; height:62px; place-items:center; margin-bottom:14px; color:var(--tql-primary); background:var(--tql-primary-soft); border-radius:50%; font-size:28px; }
.empty-state strong { font-size:16px; }.empty-state span { margin:8px 0 18px; color:var(--tql-text-tertiary); }
.calendar-toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:20px; }
.month-switch, .legend { display:flex; align-items:center; gap:12px; }.month-switch strong { min-width:120px; text-align:center; font-size:18px; }
.legend span { display:flex; align-items:center; gap:6px; color:var(--tql-text-secondary); font-size:12px; }.legend i { width:9px; height:9px; border-radius:50%; }
.legend-create { border:1px dashed var(--tql-primary); }.legend-draft { background:var(--tql-warning); }.legend-active { background:var(--tql-primary); }.legend-terminated { background:var(--tql-danger); }.legend-completed { background:var(--tql-success); }.legend-expired { background:var(--tql-text-disabled); }
.weekday-row, .calendar-grid { display:grid; grid-template-columns:repeat(7,1fr); }
.weekday-row { padding:10px 0; color:var(--tql-text-tertiary); background:var(--tql-bg-subtle); text-align:center; font-size:12px; }
.calendar-day { min-height:206px; padding:12px; border-right:1px solid var(--tql-border); border-bottom:1px solid var(--tql-border); background:var(--tql-color-white); }
.calendar-day:nth-child(7n+1) { border-left:1px solid var(--tql-border); }.calendar-day.muted { background:var(--tql-bg-muted); color:var(--tql-text-disabled); }.calendar-day.today { box-shadow:inset 0 0 0 2px var(--tql-primary); }
.day-head { display:flex; align-items:center; justify-content:space-between; }.day-head strong { font-size:15px; }.day-head span { padding:2px 6px; color:var(--tql-text-tertiary); background:var(--tql-bg-hover); border-radius:10px; font-size:10px; }
.create-day { width:100%; height:86px; margin-top:10px; color:var(--tql-primary); background:transparent; border:1px dashed var(--tql-primary-border); border-radius:6px; cursor:pointer; opacity:0; }.calendar-day:hover .create-day { opacity:1; }
.day-tasks { display:grid; grid-auto-rows:72px; gap:6px; max-height:150px; margin-top:8px; overflow:hidden; }
.day-tasks.scrollable { padding-right:5px; overflow-y:auto; overscroll-behavior:contain; scroll-snap-type:y mandatory; scrollbar-color:var(--tql-primary-border-strong) var(--tql-primary-track); scrollbar-gutter:stable; scrollbar-width:thin; }
.day-tasks.scrollable:focus-visible { outline:2px solid var(--tql-primary-border-strong); outline-offset:2px; border-radius:4px; }
.day-tasks.scrollable::-webkit-scrollbar { width:5px; }
.day-tasks.scrollable::-webkit-scrollbar-track { background:var(--tql-primary-track); border-radius:999px; }
.day-tasks.scrollable::-webkit-scrollbar-thumb { background:var(--tql-primary-border-strong); border-radius:999px; }
.day-tasks.scrollable::-webkit-scrollbar-thumb:hover { background:var(--tql-primary-hover); }
.day-tasks button { display:grid; gap:5px; min-height:72px; padding:8px; overflow:hidden; border:1px solid transparent; border-left-width:3px; border-radius:4px; scroll-snap-align:start; text-align:left; cursor:pointer; transition:box-shadow .2s ease, transform .2s ease; }.day-tasks button:hover { box-shadow:0 2px 8px rgba(0,0,0,.08); transform:translateY(-1px); }.day-tasks button:disabled { cursor:default; opacity:.8; transform:none; }.task-meta { display:flex; align-items:center; justify-content:space-between; gap:6px; min-width:0; }.task-meta > span { overflow:hidden; font-size:10px; text-overflow:ellipsis; white-space:nowrap; }.task-meta :deep(.arco-tag) { flex:none; height:20px; padding:0 6px; border-radius:10px; line-height:18px; }.day-tasks strong { overflow:hidden; color:var(--tql-text-primary); font-size:12px; text-overflow:ellipsis; white-space:nowrap; }
.calendar-task-progress { display:grid; grid-template-columns:minmax(28px,1fr) auto; align-items:center; gap:6px; min-width:0; }
.calendar-task-progress > span { color:var(--tql-text-secondary); font-size:10px; line-height:12px; white-space:nowrap; }
.calendar-task-progress :deep(.arco-progress) { min-width:0; }
.day-tasks button.task-active { border-color:var(--tql-primary-border); border-left-color:var(--tql-primary); background:var(--tql-primary-soft); }.task-active .task-meta > span { color:var(--tql-primary); }
.day-tasks button.task-draft { border-color:var(--tql-warning-border); border-left-color:var(--tql-warning); background:var(--tql-warning-soft); }.task-draft .task-meta > span { color:var(--tql-warning-hover); }
.day-tasks button.task-terminated { border-color:var(--tql-danger-border); border-left-color:var(--tql-danger); background:var(--tql-danger-soft); }.task-terminated .task-meta > span { color:var(--tql-danger-hover); }.day-tasks button.task-terminated :deep(.arco-progress-line-bar) { background:var(--tql-danger); }
.day-tasks button.task-completed { border-color:var(--tql-success-border); border-left-color:var(--tql-success); background:var(--tql-success-soft); }.task-completed .task-meta > span { color:var(--tql-success-hover); }
.day-tasks button.task-expired { border-color:var(--tql-border); border-left-color:var(--tql-text-tertiary); background:var(--tql-bg-subtle); }.task-expired .task-meta > span { color:var(--tql-text-tertiary); }
.platform-tabs { display:flex; align-items:center; gap:10px; }.platform-tabs button { display:flex; align-items:center; gap:7px; padding:8px 15px; color:var(--tql-text-secondary); background:var(--tql-color-white); border:1px solid var(--tql-border); border-radius:20px; cursor:pointer; }.platform-tabs button.active { color:var(--tql-primary); border-color:var(--tql-primary); box-shadow:0 0 0 2px var(--tql-primary-soft); }
.platform-mark, .account-platform i { display:inline-grid; width:22px; height:22px; place-items:center; color:var(--tql-color-white); border-radius:6px; font-style:normal; font-size:11px; }.all { background:var(--tql-primary); }.douyin { background:var(--tql-platform-douyin); }.kuaishou { background:var(--tql-platform-kuaishou); }.redbook { background:var(--tql-danger); }.channels { background:var(--tql-platform-channels); }
.metric-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:16px; }.metric-grid article { padding:24px 28px; border:1px solid var(--tql-border); border-radius:8px; background:var(--tql-color-white); overflow:hidden; position:relative; }.metric-grid article::after { position:absolute; right:-20px; bottom:-40px; width:130px; height:130px; border-radius:50%; opacity:.35; content:""; }
.metric-grid span { display:block; color:var(--tql-text-secondary); }.metric-grid strong { display:block; margin:8px 0; font-size:32px; }.metric-grid small { color:var(--tql-success); }.metric-blue::after { background:var(--tql-primary-border); }.metric-blue strong { color:var(--tql-primary); }.metric-purple::after { background:var(--tql-purple-soft); }.metric-purple strong { color:var(--tql-purple); }.metric-green::after { background:var(--tql-cyan-soft); }.metric-green strong { color:var(--tql-cyan); }
.panel-title { display:flex; align-items:center; justify-content:space-between; }.panel-title > div { display:flex; flex-direction:column; gap:5px; }.panel-title strong { font-size:16px; }.panel-title span { color:var(--tql-text-tertiary); font-size:12px; }
.chart { display:grid; grid-template-columns:45px 1fr; margin-top:24px; }.y-axis { display:flex; flex-direction:column; justify-content:space-between; height:280px; color:var(--tql-text-tertiary); font-size:11px; }.chart svg { width:100%; height:280px; overflow:visible; }.grid-lines line { stroke:var(--tql-border); stroke-width:1; }.x-axis { grid-column:2; display:flex; justify-content:space-between; padding-top:10px; color:var(--tql-text-tertiary); font-size:11px; }
.platform-tabs.inner { padding-bottom:16px; border-bottom:1px solid var(--tql-border); }.platform-actions { display:flex; gap:10px; margin-left:auto; }
.account-platform { display:flex; align-items:center; gap:8px; }.account-platform i { width:24px; height:24px; }
.inline-wizard-page { min-height:calc(100vh - 100px); background:var(--tql-color-white); border:1px solid var(--tql-border); border-radius:8px; }
.inline-wizard-header { display:flex; height:54px; align-items:center; gap:4px; padding:0 12px; border-bottom:1px solid var(--tql-border); }
.inline-wizard-header strong { font-size:15px; font-weight:500; }
.wizard-back { color:var(--tql-text-secondary); }
.wizard-shell { display:flex; width:100%; height:calc(100vh - 155px); min-width:0; min-height:0; overflow:hidden; flex-direction:column; background:var(--tql-color-white); }
.wizard-progress { flex:none; padding:64px 48px 56px; background:var(--tql-color-white); }
.wizard-progress :deep(.arco-steps) { max-width:880px; margin:0 auto; }
.wizard-content { flex:1; min-width:0; min-height:0; overflow-x:hidden; overflow-y:auto; width:min(880px, calc(100% - 96px)); box-sizing:border-box; margin:0 auto; padding:16px 0 64px; scrollbar-width:none; -ms-overflow-style:none; }
.wizard-content::-webkit-scrollbar { display:none; width:0; height:0; }
.wizard-card { width:100%; min-width:0; min-height:0; overflow-x:hidden; padding:0; background:var(--tql-color-white); border:0; border-radius:0; }
.wizard-card :deep(.arco-row),.wizard-card :deep(.arco-col) { min-width:0; max-width:100%; }
.wizard-card :deep(.arco-form-item) { margin-bottom:32px; }
.mode-options { display:grid; grid-template-columns:repeat(2,1fr); gap:24px; }
.mode-options button { display:grid; grid-template-columns:36px 1fr; gap:4px 16px; padding:24px; color:var(--tql-text-secondary); background:var(--tql-color-white); border:1px solid var(--tql-border); border-radius:8px; text-align:left; cursor:pointer; transition:.2s; }
.mode-options button svg { grid-row:1/3; align-self:center; color:var(--tql-text-tertiary); font-size:28px; }
.mode-options button strong { color:var(--tql-text-primary); font-size:16px; }
.mode-options button span { color:var(--tql-text-tertiary); font-size:12px; line-height:20px; }
.mode-options button:hover { border-color:var(--tql-primary-border-strong); }.mode-options button.selected { background:var(--tql-primary-subtle); border-color:var(--tql-primary); box-shadow:0 0 0 2px var(--tql-primary-soft); }.mode-options button.selected svg,.mode-options button.selected strong { color:var(--tql-primary); }
.platform-checks { display:flex; min-height:40px; align-items:center; gap:18px; }
.platform-quota-list { display:grid; width:100%; gap:12px; }
.platform-quota-row { display:grid; grid-template-columns:80px repeat(2,minmax(0,1fr)); align-items:center; gap:24px; padding:16px; background:var(--tql-bg-subtle); border-radius:6px; }
.platform-quota-row > strong { color:var(--tql-text-primary); font-size:14px; }
.platform-quota-row label { display:grid; grid-template-columns:auto minmax(80px,1fr) auto; align-items:center; gap:8px; color:var(--tql-text-secondary); white-space:nowrap; }
.platform-quota-row :deep(.arco-input-number) { width:100%; }
.delivery-period { display:flex; flex-direction:column; gap:2px; line-height:20px; }
.delivery-period span { display:block; white-space:nowrap; }
.topic-field { display:flex; width:100%; flex-direction:column; align-items:flex-start; }
.topic-editor { display:flex; width:100%; max-width:608px; gap:12px; }
.topic-editor :deep(.arco-input-wrapper) { flex:1; }
.description-heading { display:flex; align-items:center; justify-content:space-between; gap:16px; margin-bottom:8px; }
.description-heading strong { color:var(--tql-text-primary); font-size:14px; font-weight:500; }
.description-heading strong span { margin-right:4px; color:var(--tql-danger); }
.original-requirement-heading { display:flex; align-items:center; justify-content:space-between; gap:16px; margin-top:20px; }
.original-requirement-heading strong { color:var(--tql-text-primary); font-size:14px; font-weight:500; }
.original-requirement-heading strong span { margin-right:4px; color:var(--tql-danger); }
.description-ai-input { margin-top:16px; }
.topic-list { display:flex; flex-wrap:wrap; gap:8px; margin-top:12px; }
.topic-list :deep(.arco-tag) { font-size:13px; }
.topic-tip { margin-top:8px; color:var(--tql-text-tertiary); font-size:12px; line-height:20px; }
.config-stage-tabs { display:flex; gap:4px; margin-bottom:16px; padding:4px; background:var(--tql-bg-subtle); border:1px solid var(--tql-border); border-radius:8px; }
.config-stage-tabs button { min-width:0; min-height:40px; flex:1; padding:0 16px; color:var(--tql-text-secondary); font:inherit; font-size:14px; background:transparent; border:0; border-radius:6px; cursor:pointer; transition:color .2s ease,background-color .2s ease,box-shadow .2s ease; }
.config-stage-tabs button:hover { color:var(--tql-primary); background:rgba(255,255,255,.7); }
.config-stage-tabs button.active { color:var(--tql-primary); font-weight:500; background:var(--tql-color-white); box-shadow:0 1px 4px rgba(29,33,41,.12); }
.config-stage-tabs button:focus-visible { outline:2px solid var(--tql-primary); outline-offset:2px; }
.storyboard-reference-layout { display:block; margin-bottom:0; }
.storyboard-config-panel { width:100%; min-width:0; box-sizing:border-box; overflow:hidden; margin-bottom:20px; padding:0 20px 20px; background:var(--tql-color-white); border:1px solid var(--tql-border); border-radius:10px; box-shadow:0 2px 8px rgba(29,33,41,.04); }
.storyboard-content-panel { margin-bottom:20px; }
.storyboard-toolbar { display:flex; align-items:flex-start; justify-content:space-between; padding-bottom:18px; border-bottom:1px solid var(--tql-border); }
.storyboard-toolbar > div { display:flex; flex-direction:column; gap:5px; }.storyboard-toolbar strong { font-size:16px; line-height:22px; }.storyboard-toolbar span { color:var(--tql-text-tertiary); font-size:12px; line-height:18px; }
.storyboard-toolbar .storyboard-actions { flex-direction:row; align-items:center; gap:12px; }
.storyboard-toolbar .storyboard-actions label { display:flex; align-items:center; gap:10px; }
.storyboard-toolbar .storyboard-actions label > span { flex:none; color:var(--tql-text-secondary); white-space:nowrap; }
.storyboard-toolbar .storyboard-count-input { width:120px; flex:none; }
.storyboard-toolbar .storyboard-actions :deep(.arco-btn) { flex:none; white-space:nowrap; }
.storyboard-reference-layout .storyboard-toolbar { margin:0 -20px; padding:16px 20px 14px; background:var(--tql-bg-subtle); }
.basic-setting-grid { display:grid; grid-template-columns:minmax(120px,.65fr) minmax(220px,1.35fr); gap:20px; }
.basic-setting-grid .storyboard-count-input { width:100%; }
.storyboard-setting-block { min-width:0; padding-top:18px; }
.material-reference-fields { display:grid; grid-template-columns:minmax(180px,.9fr) minmax(210px,1.1fr) minmax(180px,.9fr); gap:20px; }
.setting-label { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:10px; }
.setting-label strong { color:var(--tql-text-primary); font-size:14px; font-weight:500; }
.setting-label strong i { color:var(--tql-danger); font-style:normal; }
.setting-label span { color:var(--tql-text-tertiary); font-size:12px; }
.cover-setting-block :deep(.arco-upload-wrapper),.cover-setting-block :deep(.arco-upload) { display:block; width:100%; }
.cover-example-upload { display:flex; min-height:56px; box-sizing:border-box; align-items:center; gap:12px; padding:8px 12px; background:var(--tql-color-white); border:1px dashed var(--tql-text-disabled); border-radius:6px; cursor:pointer; }
.cover-example-upload:hover { border-color:var(--tql-primary); }
.cover-example-upload > svg { flex:none; color:var(--tql-primary); font-size:22px; }
.cover-example-upload > img { width:42px; height:42px; flex:none; object-fit:cover; border-radius:4px; }
.cover-example-upload > div { display:flex; min-width:0; flex-direction:column; gap:4px; }
.cover-example-upload strong,.cover-example-upload span { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.cover-example-upload strong { color:var(--tql-text-secondary); font-size:13px; font-weight:500; }
.cover-example-upload span { color:var(--tql-text-tertiary); font-size:11px; }
.bgm-reference-block :deep(.arco-select-view) { width:100%; }
.voice-reference-block :deep(.arco-select-view) { width:100%; }
.storyboard-editor-actions { display:flex; justify-content:flex-end; margin-bottom:12px; }
.selected-bgm-list { display:grid; gap:8px; margin-top:14px; padding-top:14px; border-top:1px solid var(--tql-border); }
.selected-bgm-item { display:grid; grid-template-columns:minmax(0,1fr) 220px; align-items:center; gap:14px; padding:10px 12px; background:var(--tql-bg-subtle); border-radius:6px; }
.selected-bgm-item > div { min-width:0; }
.selected-bgm-item strong,.selected-bgm-item span { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.selected-bgm-item strong { color:var(--tql-text-primary); font-size:13px; font-weight:500; }
.selected-bgm-item span { margin-top:4px; color:var(--tql-text-tertiary); font-size:11px; }
.plan-bgm-audio { width:220px; height:30px; }
.storyboard-editor-toolbar { display:flex; align-items:center; justify-content:space-between; gap:16px; margin:2px 0 12px; }
.storyboard-editor-toolbar > div { display:flex; flex-direction:column; gap:4px; }
.storyboard-editor-toolbar strong { color:var(--tql-text-primary); font-size:15px; }
.storyboard-editor-toolbar span { color:var(--tql-text-tertiary); font-size:12px; }
.storyboard-content-form { margin-bottom:24px; padding-bottom:4px; border-bottom:1px solid var(--tql-border); }
.storyboard-editor-grid { width:100%; }
.storyboard-copy-column,.storyboard-media-column { min-width:0; }
.script-field-content { width:100%; min-width:0; }
.script-field-content :deep(.arco-textarea-wrapper) { width:100%; }
.storyboard-duration-range { display:flex; align-items:center; gap:8px; width:100%; }
.storyboard-duration-range :deep(.arco-input-number) { flex:1; min-width:0; }
.storyboard-duration-range > span { flex:none; color:var(--tql-text-secondary); font-size:13px; }
.storyboard-duration-range > i { flex:none; color:var(--tql-text-tertiary); font-style:normal; }
.storyboard-media-column :deep(.arco-upload-wrapper),.storyboard-media-column :deep(.arco-upload) { display:block; width:100%; }
.storyboard-media-column :deep(.arco-upload-list) { width:100%; }
.aspect-options { display:flex; width:100%; }
.aspect-options :deep(.arco-radio-button) { flex:1; text-align:center; }
.video-upload.large { box-sizing:border-box; margin:0 auto; transition:width .2s ease, height .2s ease; }
.video-upload.large.landscape { width:100%; height:auto; aspect-ratio:16/9; }
.video-upload.large.portrait { width:min(72%, 250px); height:auto; aspect-ratio:9/16; }
.original-requirement { margin-top:8px; }
.confirm-summary { display:grid; grid-template-columns:repeat(3,1fr); margin-bottom:24px; border-top:1px solid var(--tql-border); border-left:1px solid var(--tql-border); }
.confirm-summary > div { min-height:88px; padding:18px 20px; border-right:1px solid var(--tql-border); border-bottom:1px solid var(--tql-border); }
.confirm-summary span,.confirm-summary strong { display:block; }.confirm-summary span { margin-bottom:8px; color:var(--tql-text-tertiary); font-size:12px; }.confirm-summary strong { font-size:15px; }
.confirm-copy { margin-top:22px; padding:20px; background:var(--tql-bg-subtle); border-radius:6px; }.confirm-copy p { margin:6px 0 18px; color:var(--tql-text-secondary); line-height:1.7; }.confirm-copy p:last-child { margin-bottom:0; }
.wizard-footer { flex:none; width:100%; box-sizing:border-box; padding:16px 32px; background:var(--tql-color-white); border-top:1px solid var(--tql-border); box-shadow:0 -2px 8px rgba(0,0,0,.03); }
.wizard-footer-actions { display:flex; width:100%; min-width:0; min-height:32px; align-items:center; justify-content:flex-end; gap:8px; margin:0; }
.wizard-footer-actions > .arco-space { margin-left:0; }
.form-section { padding:20px 2px 26px; border-bottom:1px solid var(--tql-border); }.form-section:first-child { padding-top:0; }.form-section h3 { display:flex; align-items:center; gap:9px; margin:0 0 20px; font-size:16px; }.form-section h3 span { display:grid; width:24px; height:24px; place-items:center; color:var(--tql-color-white); background:var(--tql-primary); border-radius:50%; font-size:12px; }
.section-title-row { display:flex; align-items:center; justify-content:space-between; }.full-width { width:100%; }.release-date-item :deep(.arco-form-item-extra) { color:var(--tql-danger); }.task-start-setting { display:flex; align-items:center; gap:18px; }.task-start-picker { width:280px; }.task-start-setting :deep(.arco-checkbox) { flex:none; }.account-selector { display:flex; align-items:center; justify-content:flex-start; gap:18px; width:100%; padding:12px 16px; background:var(--tql-bg-subtle); border-radius:6px; }.account-selector > span { margin-left:auto; color:var(--tql-text-secondary); }.account-selector strong { color:var(--tql-primary); }
.duration-tip { margin-top:6px; color:var(--tql-text-tertiary); font-size:12px; }.video-upload { display:flex; width:170px; height:230px; flex-direction:column; align-items:center; justify-content:center; gap:8px; color:var(--tql-text-tertiary); background:var(--tql-bg-subtle); border:1px dashed var(--tql-text-disabled); border-radius:8px; }.video-upload svg { color:var(--tql-primary); font-size:28px; }.video-upload strong { color:var(--tql-text-secondary); }.video-upload span { font-size:11px; }
.drawer-footer { display:flex; align-items:center; justify-content:space-between; width:100%; }.drawer-footer > span { color:var(--tql-text-tertiary); font-size:12px; }
.detail-hero { display:flex; align-items:flex-start; justify-content:space-between; padding:4px 0 22px; }.detail-hero h2 { margin:12px 0 6px; }.detail-hero p { margin:0; color:var(--tql-text-tertiary); }.detail-page > h3 { margin:26px 0 12px; }.detail-copy { padding:16px; color:var(--tql-text-secondary); line-height:1.8; background:var(--tql-bg-subtle); border-radius:6px; }
.delivery-summary { display:flex; align-items:center; gap:24px; margin-bottom:16px; padding:12px 16px; background:var(--tql-bg-subtle); }.delivery-summary button { margin-left:auto; }
.employee-search { border-radius:8px; background:var(--tql-bg-subtle); }
.org-selector { display:grid; grid-template-columns:minmax(0,1.75fr) minmax(300px,.85fr); height:520px; margin-top:16px; overflow:hidden; border:1px solid var(--tql-border); border-radius:10px; background:var(--tql-color-white); }
.org-tree,.selected-org { min-width:0; padding:20px; overflow:hidden; }
.org-tree { border-right:1px solid var(--tql-border); }
.selected-org { display:flex; flex-direction:column; background:linear-gradient(180deg,var(--tql-bg-highlight) 0%,var(--tql-bg-subtle) 100%); }
.selector-heading { display:flex; align-items:center; justify-content:space-between; height:28px; margin-bottom:14px; }
.selector-heading strong { color:var(--tql-text-primary); font-size:15px; font-weight:600; }
.selector-heading span { padding:3px 9px; color:var(--tql-primary); font-size:12px; background:var(--tql-primary-soft); border-radius:10px; }
.employee-tree-loading { display:block; height:420px; overflow:auto; padding-right:4px; }
.selected-employee-list { display:grid; gap:10px; padding-right:4px; overflow:auto; }
.selected-employee { position:relative; padding:13px 34px 12px 14px; border:1px solid var(--tql-border-primary-soft); border-radius:8px; background:var(--tql-color-white); box-shadow:0 2px 8px rgba(22,93,255,.06); }
.selected-employee span,.selected-employee small { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.selected-employee span { color:var(--tql-text-primary); font-size:14px; font-weight:600; }
.selected-employee-meta { display:grid; gap:4px; margin-top:7px; }
.selected-employee small { color:var(--tql-text-sidebar-secondary); font-size:12px; }
.selected-employee button { position:absolute; top:12px; right:10px; width:22px; height:22px; padding:0; color:var(--tql-text-tertiary); background:var(--tql-bg-hover); border:0; border-radius:50%; cursor:pointer; }
.selected-employee button:hover { color:var(--tql-danger); background:var(--tql-border-danger-soft); }
.selection-tip { margin:auto 0 0; padding-top:14px; color:var(--tql-text-tertiary); font-size:12px; line-height:1.6; }
.personnel-import-toolbar { display:flex; align-items:center; gap:12px; margin:16px 0; padding:14px 16px; background:var(--tql-bg-subtle); border-radius:8px; }
.personnel-file-name { min-width:0; flex:1; overflow:hidden; color:var(--tql-text-secondary); text-overflow:ellipsis; white-space:nowrap; }
.personnel-import-footer { display:flex; align-items:center; justify-content:space-between; margin-top:16px; padding-top:16px; color:var(--tql-text-secondary); border-top:1px solid var(--tql-border); }
.selected-count { display:inline-flex; align-items:center; gap:5px; color:var(--tql-text-secondary); white-space:nowrap; }.selected-count strong { color:var(--tql-primary); }.selected-count :deep(.arco-link) { margin-left:5px; }
.selected-personnel-alert { margin-bottom:16px; }
.account-form-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:0 20px; }
.account-status-tip { align-self:start; margin-top:30px; }
.import-drop { display:flex; height:180px; flex-direction:column; align-items:center; justify-content:center; gap:10px; }.import-drop svg { color:var(--tql-primary); font-size:32px; }.import-drop span { color:var(--tql-text-tertiary); font-size:12px; }
.panel-toolbar { display:flex; align-items:center; justify-content:space-between; gap:16px; margin-bottom:16px; }
.panel-toolbar > div { display:flex; align-items:baseline; gap:10px; }
.panel-toolbar strong { font-size:16px; }
.panel-toolbar span { color:var(--tql-text-tertiary); font-size:12px; }
.bgm-name { display:flex; min-width:240px; flex-direction:column; gap:8px; }
.bgm-name audio { width:250px; height:32px; }
.bgm-copyright { display:flex; flex-direction:column; gap:4px; }
.bgm-copyright span { max-width:230px; overflow:hidden; color:var(--tql-text-tertiary); font-size:12px; text-overflow:ellipsis; white-space:nowrap; }
.bgm-form { margin-top:20px; }
.bgm-form :deep(.arco-input-number) { width:100%; }
.material-workbench { display:grid; grid-template-columns:220px minmax(0,1fr); height:calc(100vh - 100px); min-height:480px; overflow:hidden; background:var(--tql-color-white); border:1px solid var(--tql-border); border-radius:var(--tql-radius-card); }
.material-type-rail { display:flex; min-height:0; flex-direction:column; gap:8px; padding:12px; border-right:1px solid var(--tql-border); background:var(--tql-color-white); }
.material-type-rail .folder-search-input { flex:0 0 auto; }
.material-folder-tree { flex:1 1 auto; min-height:0; overflow:auto; padding:4px 0; }
.material-folder-tree :deep(.arco-tree-node-title-block) { border-radius:var(--tql-radius-control); }
.material-folder-tree :deep(.arco-tree-node-selected .arco-tree-node-title) { color:var(--tql-primary); background:var(--tql-primary-soft); font-weight:500; }
.folder-create-btn { flex:0 0 auto; margin-top:auto; }
.material-content { display:grid; min-width:0; align-content:start; gap:20px; padding:16px; overflow-y:auto; }
.material-preview-cell { display:flex; min-width:300px; align-items:center; gap:12px; }
.material-preview-cell video,.material-preview-cell img { width:104px; height:64px; flex:0 0 auto; border:1px solid var(--tql-border); border-radius:6px; background:var(--tql-bg-hover); object-fit:cover; }
.material-preview-cell > div,.material-file-info { display:flex; min-width:0; flex-direction:column; gap:5px; }
.material-preview-cell strong { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.material-preview-cell span,.material-file-info span { overflow:hidden; color:var(--tql-text-tertiary); font-size:12px; text-overflow:ellipsis; white-space:nowrap; }
.material-form { margin-top:20px; }
.form-helper { margin-top:8px; color:var(--tql-text-tertiary); font-size:12px; }
@media (max-width:1280px) { .search-panel form { grid-template-columns:repeat(2,minmax(240px,1fr)); } }
@media (max-width:1100px) { .material-reference-fields { grid-template-columns:1fr 1fr; }.voice-reference-block { grid-column:1 / -1; } }
@media (max-width:720px) { .basic-setting-grid,.material-reference-fields { grid-template-columns:1fr; }.selected-bgm-item { grid-template-columns:1fr; }.plan-bgm-audio { width:100%; } }
@media (max-width:720px) {
  .calendar-toolbar { align-items:flex-start; flex-direction:column; gap:var(--tql-space-3); }
  .calendar-panel :deep(.arco-card-body) { overflow-x:auto; }
  .weekday-row, .calendar-grid { min-width:980px; }
}
.folder-breadcrumb { display:flex; flex:1; align-items:center; gap:8px; margin-right:24px; }
.folder-breadcrumb .breadcrumb-divider { color:var(--tql-text-tertiary); }
.folder-breadcrumb .breadcrumb-current { color:var(--tql-text-primary); font-weight:500; }
.folder-search-panel { align-self:start; }
.folder-search-panel :deep(.arco-card-body) { align-items:center; gap:14px; padding:8px 14px; }
.folder-search-panel form { grid-template-columns:repeat(4, minmax(150px, 1fr)); gap:12px 16px; }
.material-folder-grid { display:grid; grid-template-columns:repeat(auto-fill, minmax(220px, 1fr)); gap:16px; padding:4px; }
.material-content .search-panel, .material-content .data-panel { border:none; }
.material-folder-card { position:relative; display:flex; align-items:center; gap:14px; padding:18px; background:var(--tql-color-white); border:1px solid var(--tql-border); border-radius:8px; cursor:pointer; transition:all .2s ease; }
.material-folder-card:hover { border-color:var(--tql-primary-border-strong); box-shadow:0 2px 8px rgba(22,93,255,.08); }
.material-folder-card .folder-icon { display:grid; width:48px; height:48px; place-items:center; color:var(--tql-primary); background:var(--tql-primary-soft); border-radius:8px; font-size:24px; }
.material-folder-card .folder-info { display:flex; min-width:0; flex-direction:column; gap:5px; flex:1; }
.material-folder-card .folder-info strong { overflow:hidden; font-size:15px; font-weight:500; text-overflow:ellipsis; white-space:nowrap; }
.material-folder-card .folder-info span { color:var(--tql-text-tertiary); font-size:12px; }
.material-folder-card .folder-status { position:absolute; right:12px; top:12px; }
.material-folder-card .folder-more-btn { position:absolute; right:8px; bottom:8px; width:28px; height:28px; padding:0; color:var(--tql-text-tertiary); border-radius:6px; }
.material-folder-card .folder-more-btn:hover { color:var(--tql-primary); background:var(--tql-bg-hover); }
.material-folder-card.disabled { background:var(--tql-bg-subtle); }
.material-folder-card.disabled .folder-icon { color:var(--tql-text-tertiary); background:var(--tql-bg-hover); }
.move-form { margin-top:16px; }
</style>
