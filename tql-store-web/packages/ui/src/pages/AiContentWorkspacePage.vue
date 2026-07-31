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
              <a-option value="已终止">已终止</a-option>
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
            <a-button v-if="can(P.planExportDelivery)" @click="exportDetails"><template #icon><IconDownload /></template>导出人员</a-button>
            <a-button v-if="can(P.planExportMaterial)" @click="exportMaterials"><template #icon><IconDownload /></template>导出素材收集</a-button>
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
          <template #progress="{ record }">
            <div class="progress-cell">
              <a-progress :percent="record.progress / 100" size="small" :show-text="false" />
              <span>{{ record.completed }}/{{ record.accounts }}</span>
            </div>
          </template>
          <template #actions="{ record }">
            <a-space size="mini">
              <a-link v-if="can(P.planUpdate) && !['已终止', '已完成', '已过期'].includes(record.status)" @click="openEdit(record)">编辑</a-link>
              <a-link v-if="can(P.planView)" @click="viewPlan(record)">查看</a-link>
              <a-link v-if="can(P.planDelivery)" @click="viewDelivery(record)">人员</a-link>
              <a-link v-if="can(P.planCancel) && !['草稿', '待开始', '已终止', '已完成', '已过期'].includes(record.status)" status="danger" @click="cancelPlan(record)">取消</a-link>
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
            <a-button shape="circle" @click="shiftMonth(-1)"><IconLeft /></a-button>
            <strong>{{ calendarYear }}年{{ calendarMonth }}月</strong>
            <a-button shape="circle" @click="shiftMonth(1)"><IconRight /></a-button>
            <a-button @click="goToday">今天</a-button>
          </div>
          <div class="legend">
            <span><i class="legend-create"></i>可创建</span>
            <span><i class="legend-draft"></i>草稿</span>
            <span><i class="legend-active"></i>进行中</span>
            <span><i class="legend-terminated"></i>已终止</span>
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
            <div v-if="cell.tasks.length" class="day-tasks">
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

    <section v-else class="module-content">
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
          <template #actions="{ record }"><a-space size="mini"><a-link v-if="can(P.accountDetailView)" @click="openAccountEditor(record, true)">查看</a-link><a-link v-if="can(P.accountUpdate)" @click="openAccountEditor(record)">编辑</a-link><a-link v-if="can(P.accountDelete)" status="danger" @click="removeAccounts([record.id])">删除</a-link></a-space></template>
        </a-table>
      </a-card>
      </section>
    </template>

    <section v-else class="inline-wizard-page">
      <header class="inline-wizard-header">
        <a-button class="wizard-back" type="text" shape="circle" aria-label="返回发布计划" @click="leaveCreate">
          <template #icon><IconLeft /></template>
        </a-button>
        <strong>{{ editingActivityId ? '编辑发布计划' : '创建发布计划' }}</strong>
      </header>
      <div ref="wizardShellRef" class="wizard-shell">
        <div class="wizard-progress">
          <a-steps :current="wizardStep" label-placement="vertical">
            <a-step title="活动信息" description="设置计划名称与任务类型" />
            <a-step title="内容配置" description="配置时间、人员和发布内容" />
            <a-step :title="planForm.type === '半原创' ? '分镜模板' : '拍摄要求'" description="完善员工拍摄执行标准" />
            <a-step title="确认下发" description="核对配置并创建任务" />
          </a-steps>
        </div>

        <main class="wizard-content">
          <div v-if="wizardStep === 1" class="wizard-card">
            <a-form :model="planForm" layout="vertical">
              <a-grid :cols="2" :col-gap="24">
                <a-grid-item>
                  <a-form-item label="计划名称" required>
                    <a-input v-model="planForm.name" size="large" placeholder="例如：夏日冰饮重点商品推广" :max-length="80" show-word-limit />
                  </a-form-item>
                </a-grid-item>
                <a-grid-item>
                  <a-form-item label="发布方式" required>
                    <a-select v-model="planForm.deliveryMode" size="large"><a-option value="员工任务">员工任务</a-option></a-select>
                  </a-form-item>
                </a-grid-item>
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
                  <a-form-item class="release-date-item" label="发布日期" required extra="为确保流程正常进行，发布日期必须大于当前时间5天">
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
                      <a-checkbox value="抖音">抖音</a-checkbox><a-checkbox value="快手">快手</a-checkbox><a-checkbox value="小红书">小红书</a-checkbox><a-checkbox value="视频号">视频号</a-checkbox>
                    </a-checkbox-group>
                  </a-form-item>
                </a-grid-item>
              </a-grid>
              <a-form-item label="任务开始时间" required extra="到达该时间后，系统将通过企业微信自建应用通知已下发员工">
                <a-date-picker
                  v-model="planForm.taskStartTime"
                  class="task-start-picker"
                  size="large"
                  show-time
                  format="YYYY-MM-DD HH:mm:ss"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="请选择任务开始时间"
                  :disabled-date="disabledTaskStartDate"
                  :disabled-time="disabledTaskStartTime"
                  @change="validateTaskStartTime"
                />
              </a-form-item>
              <a-form-item label="发布账户" required>
                <div class="account-selector">
                  <a-radio-group v-model="planForm.accountMode"><a-radio value="组织">通过组织选择</a-radio><a-radio v-if="can(P.planImportEmployee)" value="人员">导入人员</a-radio></a-radio-group>
                  <a-button v-if="can(P.planSelectEmployee) && planForm.accountMode === '组织'" @click="openEmployeeSelector">选择组织/人员</a-button>
                  <a-button v-if="can(P.planImportEmployee) && planForm.accountMode === '人员'" @click="openPersonnelImport"><template #icon><IconUpload /></template>导入人员</a-button>
                  <span class="selected-count">
                    已选择 <strong>{{ planForm.employeeCount }}</strong> 名员工
                    <a-link v-if="planForm.employeeCount" @click="selectedPersonnelVisible = true">查看</a-link>
                  </span>
                </div>
              </a-form-item>
            </a-form>
          </div>

          <div v-else-if="wizardStep === 3" class="wizard-card">
            <a-form :model="planForm" layout="vertical" class="storyboard-content-form">
              <a-form-item label="活动文案" required>
                <a-textarea v-model="planForm.description" placeholder="请描述活动主题、商品卖点和传播目标，AI将据此生成分镜台词" :max-length="1000" show-word-limit :auto-size="{ minRows: 4, maxRows: 7 }" />
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
              <div class="storyboard-toolbar">
                <div><strong>分镜模板</strong><span>员工将按分镜顺序完成拍摄与上传</span></div>
                <div class="storyboard-actions">
                  <span>分镜数量</span>
                  <a-input-number v-model="planForm.storyboardCount" :min="1" :max="8" @change="syncStoryboards" />
                  <a-button v-if="can(P.planAiScript)" type="outline" :loading="generating" @click="generateScripts"><template #icon><IconRobot /></template>AI生成全部分镜脚本</a-button>
                </div>
              </div>
              <a-tabs v-model:active-key="activeStoryboard" type="card-gutter">
                <a-tab-pane v-for="(storyboard, index) in storyboards" :key="index" :title="`分镜 ${index + 1}`">
                  <a-form :model="storyboard" layout="vertical">
                    <a-grid class="storyboard-editor-grid" :cols="25" :col-gap="28">
                      <a-grid-item :span="13" class="storyboard-copy-column">
                        <a-form-item label="拍摄要求" required><a-textarea v-model="storyboard.requirement" placeholder="例如：在光线明亮的场景拍摄，产品保持在画面中央" :auto-size="{ minRows: 5 }" /></a-form-item>
                        <a-form-item label="分镜台词" required>
                          <a-textarea v-model="storyboard.script" placeholder="点击“AI生成全部分镜脚本”自动生成，也可手动修改" :max-length="300" show-word-limit :auto-size="{ minRows: 5 }" />
                          <div class="duration-tip">预计话术时长 {{ estimateDuration(storyboard.script) }} 秒</div>
                        </a-form-item>
                      </a-grid-item>
                      <a-grid-item :span="12" class="storyboard-media-column">
                        <a-form-item label="画面方向" required>
                          <a-radio-group v-model="storyboard.sampleAspect" type="button" class="aspect-options">
                            <a-radio value="portrait">竖版 9:16</a-radio>
                            <a-radio value="landscape">横版 16:9</a-radio>
                          </a-radio-group>
                        </a-form-item>
                        <a-form-item label="样例视频" required>
                          <a-upload :auto-upload="false" :limit="1" accept="video/mp4" @change="files => uploadStoryboardSample(index, files)">
                            <template #upload-button>
                              <div class="video-upload large" :class="storyboard.sampleAspect === 'landscape' ? 'landscape' : 'portrait'">
                                <IconUpload />
                                <strong>{{ storyboard.uploading ? '正在上传...' : storyboard.sampleVideoUrl ? '重新上传样例视频' : '上传样例视频' }}</strong>
                                <span v-if="storyboard.sampleVideoName">{{ storyboard.sampleVideoName }}<br />{{ sampleAspectText(storyboard.sampleAspect) }}，员工端按比例展示</span>
                                <span v-else>支持 MP4，最大 200MB<br />请上传{{ sampleAspectText(storyboard.sampleAspect) }}视频</span>
                              </div>
                            </template>
                          </a-upload>
                        </a-form-item>
                        <a-form-item label="样例封面URL">
                          <a-input v-model="storyboard.sampleCoverUrl" placeholder="可选，视频封面图片URL" />
                        </a-form-item>
                      </a-grid-item>
                    </a-grid>
                  </a-form>
                </a-tab-pane>
              </a-tabs>
            </template>
            <template v-else>
              <a-alert type="info">原创任务不配置分镜，员工将根据以下要求自主完成短视频创作。</a-alert>
              <a-form-item label="原创拍摄要求" required class="original-requirement">
                <a-textarea v-model="planForm.originalRequirement" placeholder="请说明时长、场景、出镜、商品展示、口播等要求" :auto-size="{ minRows: 10 }" />
              </a-form-item>
            </template>
          </div>

          <div v-else class="wizard-card confirm-card">
            <div class="confirm-summary">
              <div><span>计划名称</span><strong>{{ planForm.name }}</strong></div>
              <div><span>任务类型</span><strong>{{ planForm.type }}</strong></div>
              <div><span>发布日期</span><strong>{{ planForm.dateRange.join(' 至 ') || '未设置' }}</strong></div>
              <div><span>任务开始时间</span><strong>{{ planForm.taskStartTime || '未设置' }}</strong></div>
              <div><span>下发员工</span><strong>{{ planForm.employeeCount }} 人</strong></div>
              <div><span>发布平台</span><strong>{{ planForm.platforms.join('、') }}</strong></div>
              <div><span>{{ planForm.type === '半原创' ? '分镜数量' : '创作方式' }}</span><strong>{{ planForm.type === '半原创' ? `${planForm.storyboardCount} 个` : '员工原创' }}</strong></div>
            </div>
            <a-alert type="warning">提交后将按选定账号创建员工任务。建议确认人员范围、发布日期和分镜内容无误后再下发。</a-alert>
            <div class="confirm-copy"><strong>活动文案</strong><p>{{ planForm.description }}</p></div>
            <div v-if="topicList.length" class="confirm-copy"><strong>发布话题</strong><p>{{ formattedTopics }}</p></div>
          </div>
        </main>
        <div class="wizard-footer">
          <div class="wizard-footer-actions">
            <a-button v-if="!editingActivityId && can(P.planSave)" type="text" :loading="submitting" @click="saveDraft">保存草稿</a-button>
            <a-space>
              <a-button v-if="wizardStep > 1" @click="wizardStep--">上一步</a-button>
              <a-button v-if="wizardStep < 4" type="primary" @click="nextWizardStep">下一步</a-button>
              <a-button v-else-if="editingActivityId ? can(P.planUpdate) : can(P.planPublish)" type="primary" :loading="submitting" @click="submitPlan">{{ editingActivityId ? '保存修改' : '确认并下发' }}</a-button>
            </a-space>
          </div>
        </div>
      </div>
    </section>

    <a-drawer v-model:visible="detailVisible" title="发布计划详情" :width="760">
      <div v-if="selectedPlan" class="detail-page">
        <div class="detail-hero">
          <div><a-tag :color="selectedPlan.type === '半原创' ? 'arcoblue' : 'purple'">{{ selectedPlan.type }}</a-tag><h2>{{ selectedPlan.name }}</h2><p>计划ID：{{ selectedPlan.id }} · 创建人：{{ selectedPlan.owner }}</p></div>
          <span class="status-dot" :class="statusClass(selectedPlan.status)">{{ selectedPlan.status }}</span>
        </div>
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="任务时间">{{ selectedPlan.startDate }} 至 {{ selectedPlan.endDate }}</a-descriptions-item>
          <a-descriptions-item label="发布账号">{{ selectedPlan.accounts }} 个</a-descriptions-item>
          <a-descriptions-item label="执行进度">{{ selectedPlan.completed }}/{{ selectedPlan.accounts }}（{{ selectedPlan.progress }}%）</a-descriptions-item>
          <a-descriptions-item label="发布平台">抖音、快手、小红书、视频号</a-descriptions-item>
        </a-descriptions>
        <h3>执行进度</h3><a-progress :percent="selectedPlan.progress / 100" />
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
          <a-form-item field="employeeId" label="归属员工" required><a-select v-model="accountForm.employeeId" allow-search :loading="employeesLoading" placeholder="请选择已启用且可登录的员工" @change="syncAccountOrganization"><a-option v-for="employee in employees" :key="employee.id" :value="employee.id">{{ employee.displayName }}（{{ employee.employeeNumber || employee.username }}）</a-option></a-select></a-form-item>
          <a-alert class="account-status-tip" type="info">账号保存后状态为“待校验”。接入对应开放平台后，系统将根据授权或接口校验结果自动更新状态。</a-alert>
        </div>
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
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { Message, Modal, type TableColumnData } from '@arco-design/web-vue';
import {
  IconArrowRise, IconDownload, IconLeft, IconPlus, IconRefresh, IconRight, IconRobot, IconSearch,
  IconUpload, IconVideoCamera
} from '@arco-design/web-vue/es/icon';
import {
  createContentAccount, createContentActivity, createContentPlan, deleteContentAccounts, deleteContentActivity,
  fetchActivityPlans, fetchContentAccounts, fetchContentActivities, fetchContentDeliveryTasks,
  downloadPersonnelImportTemplate, fetchContentAccountOrganizations, fetchContentAccountUsers,
  fetchContentTaskOrganizations, fetchContentTaskUsers, fetchContentVideoPerformance, importContentAccounts,
  publishContentPlan, terminateContentActivity, updateContentAccount, updateContentActivity,
  uploadContentSampleVideo, validatePersonnelImport
} from '@tql-store/api';
import { usePermission } from '@tql-store/auth';
import type { ContentAccountItem, ContentAccountPayload, ContentDeliveryItem, ContentVideoPerformanceItem, OrganizationOption, PersonnelImportResult, UserItem } from '@tql-store/shared';
import OrganizationCollapseNode from '../components/OrganizationCollapseNode.vue';

type ModuleKey = 'plans' | 'calendar' | 'analytics' | 'accounts';
type PlanRow = {
  id: string | number; type: string; name: string; owner: string; status: string;
  accounts: number; completed: number; progress: number; startDate: string; endDate: string; startTime: string; tone?: string;
  objective?: string; rawStatus?: string;
};
type CalendarTone = 'draft' | 'active' | 'terminated' | 'completed' | 'expired';

const activityStatusMeta: Record<string, { label: string; tone: CalendarTone }> = {
  DRAFT: { label: '草稿', tone: 'draft' },
  ACTIVE: { label: '进行中', tone: 'active' },
  TERMINATED: { label: '已终止', tone: 'terminated' },
  COMPLETED: { label: '已完成', tone: 'completed' },
  EXPIRED: { label: '已过期', tone: 'expired' }
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
  accountDelete: 'merchant:content:account:delete'
} as const;
const loading = ref(false);
const submitting = ref(false);
const filters = reactive({ type: '', keyword: '', status: '', owner: '', dateRange: [] as string[] });
const appliedFilters = reactive({ ...filters });
const backendPlans = ref<PlanRow[]>([]);
const allPlans = computed(() => backendPlans.value);
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
  { title: '选定账号数', dataIndex: 'accounts', width: 110 }, { title: '任务执行进度', slotName: 'progress', width: 190 },
  { title: '操作', slotName: 'actions', width: 220, fixed: 'right' }
];

async function loadPlans() {
  loading.value = true;
  try {
    const rows = await fetchContentActivities();
    backendPlans.value = rows.map(item => {
      const waiting = item.status === 'ACTIVE' && new Date(item.startTime).getTime() > Date.now();
      const statusMeta = waiting
        ? { label: '待开始', tone: 'draft' as CalendarTone }
        : activityStatusMeta[item.status] || { label: item.status, tone: 'draft' as CalendarTone };
      return {
        id: item.id, type: item.objective?.includes('原创') ? '原创' : '半原创', name: item.name,
        owner: item.ownerName, status: statusMeta.label,
        objective: item.objective, rawStatus: item.status,
        accounts: item.employeeCount, completed: item.completedCount,
        progress: item.employeeCount ? Math.round(item.completedCount / item.employeeCount * 100) : 0,
        startDate: item.startTime.slice(0, 10), endDate: item.endTime.slice(0, 10), startTime: item.startTime,
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
  return status === '进行中' ? 'status-active' : status === '已终止' ? 'status-terminated' : status === '已完成' ? 'status-done' : 'status-draft';
}
function statusColor(status: string) {
  return ({ '进行中': 'arcoblue', '已终止': 'red', '已完成': 'green', '已过期': 'gray', '草稿': 'orange' } as Record<string, string>)[status] || 'gray';
}
function calendarStatusColor(tone?: string) {
  return ({ active: 'blue', terminated: 'red', completed: 'green', expired: 'gray', draft: 'orange' } as Record<string, string>)[tone || 'draft'];
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
watch(
  activeModule,
  async (module) => {
    createVisible.value = false;
    if (module === 'accounts') await loadAccounts();
    else if (module === 'analytics') await loadVideoReports();
    else await loadPlans();
  },
  { immediate: true }
);
const wizardStep = ref(1);
const planForm = reactive({
  name: '', deliveryMode: '员工任务', type: '半原创', dateRange: [] as string[], accountMode: '组织',
  employeeCount: 0, platforms: ['抖音'], description: '', taskCopy: '', topic: '', title: '', taskStartTime: '',
  storyboardCount: 3, originalRequirement: ''
});
const topicInput = ref('');
const topicList = ref<string[]>([]);
const formattedTopics = computed(() => topicList.value.map(topic => `#${topic}#`).join(' '));

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
  sampleVideoUrl?: string;
  sampleVideoName?: string;
  sampleCoverUrl?: string;
  sampleAspect?: 'portrait' | 'landscape' | '';
  uploading?: boolean;
};
const emptyStoryboard = (): StoryboardDraft => ({
  requirement: '',
  script: '',
  sampleVideoUrl: '',
  sampleVideoName: '',
  sampleCoverUrl: '',
  sampleAspect: 'portrait',
  uploading: false
});
const storyboards = ref<StoryboardDraft[]>([emptyStoryboard(), emptyStoryboard(), emptyStoryboard()]);
const activeStoryboard = ref(0);
const generating = ref(false);
function openCreate(date?: string) { editingActivityId.value = undefined; resetPlanForm(); wizardStep.value = 1; if (date) planForm.dateRange = [date, date]; createVisible.value = true; loadEmployees(); }
function leaveCreate() { createVisible.value = false; editingActivityId.value = undefined; wizardStep.value = 1; }
const wizardShellRef = ref<HTMLElement | null>(null);
function forwardWizardWheel(event: WheelEvent) {
  const shell = wizardShellRef.value;
  const content = shell?.querySelector<HTMLElement>('.wizard-content');
  if (!shell || !content || content.contains(event.target as Node)) return;
  content.scrollTop += event.deltaY;
}
onMounted(() => {
  wizardShellRef.value?.addEventListener('wheel', forwardWizardWheel, { passive: true });
});
onBeforeUnmount(() => {
  wizardShellRef.value?.removeEventListener('wheel', forwardWizardWheel);
});
function resetPlanForm() { Object.assign(planForm, { name: '', deliveryMode: '员工任务', type: '半原创', dateRange: [], accountMode: '组织', employeeCount: 0, platforms: ['抖音'], description: '', taskCopy: '', topic: '', title: '', taskStartTime: '', storyboardCount: 3, originalRequirement: '' }); topicInput.value = ''; topicList.value = []; selectedEmployeeIds.value = []; importedPersonnelDetails.value = {}; storyboards.value = [emptyStoryboard(), emptyStoryboard(), emptyStoryboard()]; }
function instructionValue(lines: string[], label: string) {
  return lines.find(line => line.startsWith(`${label}：`))?.slice(label.length + 1) || '';
}
async function openEdit(record: PlanRow) {
  editingActivityId.value = Number(record.id);
  resetPlanForm();
  submitting.value = true;
  try {
    await loadEmployees();
    const [plans, personnel] = await Promise.all([
      fetchActivityPlans(Number(record.id)),
      fetchContentDeliveryTasks(Number(record.id))
    ]);
    const plan = plans[0];
    if (!plan) throw new Error('计划内容不存在');
    const lines = plan.taskInstruction.split('\n').filter(Boolean);
    const platforms = instructionValue(lines, '发布平台').split('、').filter(Boolean);
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
            sampleVideoUrl: values['样例视频'] || '',
            sampleVideoName: values['样例视频'] ? '已上传样例视频' : '',
            sampleCoverUrl: values['样例封面'] || '',
            sampleAspect: normalizeSampleAspect(values['样例比例']) || 'portrait'
          };
        })
      : [];
    Object.assign(planForm, {
      name: record.name,
      type: plan.creationMode === 'SELF_CREATED' ? '原创' : '半原创',
      dateRange: [record.startDate, record.endDate],
      taskStartTime: record.startTime.replace('T', ' ').slice(0, 19),
      accountMode: '组织',
      employeeCount: new Set(personnel.map(item => item.employeeId)).size,
      platforms: platforms.length ? platforms : ['抖音'],
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
function syncStoryboards(value?: number) { const count = value || planForm.storyboardCount; while (storyboards.value.length < count) storyboards.value.push(emptyStoryboard()); storyboards.value = storyboards.value.slice(0, count); if (activeStoryboard.value >= count) activeStoryboard.value = count - 1; }
async function generateScripts() {
  if (!planForm.description) { Message.warning('请先填写活动文案'); return; }
  generating.value = true;
  await new Promise(resolve => setTimeout(resolve, 650));
  const scripts = ['今天给大家推荐一款夏日必备好物，清爽体验从第一眼就开始。', '近距离展示产品细节和核心卖点，真实分享使用感受。', '现在到店即可参与活动，带上话题发布你的专属体验。'];
  storyboards.value.forEach((item, index) => { item.script = scripts[index % scripts.length]; item.requirement ||= ['正面半身出镜，环境明亮，产品置于画面中央', '切换近景，缓慢展示包装和使用过程', '回到人物正面，口播活动信息并自然收尾'][index % 3]; });
  generating.value = false; Message.success('分镜脚本已生成，可继续修改');
}
function estimateDuration(script: string) { return Math.max(0, Math.ceil(script.length / 4)); }
function minimumReleaseTime() {
  return Date.now() + 5 * 24 * 60 * 60 * 1000;
}
function disabledReleaseDate(date: Date) {
  return date.getTime() <= minimumReleaseTime();
}
function taskStartBounds() {
  const now = new Date();
  const publishStart = planForm.dateRange[0]
    ? new Date(`${planForm.dateRange[0]}T00:00:00`)
    : now;
  const min = publishStart.getTime() > now.getTime() ? publishStart : now;
  const max = planForm.dateRange[1]
    ? new Date(`${planForm.dateRange[1]}T23:59:59`)
    : undefined;
  return { min, max };
}
function sameLocalDate(left: Date, right: Date) {
  return left.getFullYear() === right.getFullYear()
    && left.getMonth() === right.getMonth()
    && left.getDate() === right.getDate();
}
function disabledTaskStartDate(date?: Date) {
  if (!date) return false;
  const { min, max } = taskStartBounds();
  const dayStart = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
  const minDay = new Date(min.getFullYear(), min.getMonth(), min.getDate()).getTime();
  const maxDay = max ? new Date(max.getFullYear(), max.getMonth(), max.getDate()).getTime() : undefined;
  return dayStart < minDay || (maxDay !== undefined && dayStart > maxDay);
}
function disabledTaskStartTime(date?: Date) {
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
  if (!planForm.taskStartTime) return false;
  const value = new Date(planForm.taskStartTime.replace(' ', 'T'));
  const { min, max } = taskStartBounds();
  return !Number.isNaN(value.getTime())
    && value.getTime() >= min.getTime()
    && (!max || value.getTime() <= max.getTime());
}
function validateTaskStartTime() {
  if (!planForm.taskStartTime || taskStartIsValid()) return;
  planForm.taskStartTime = '';
  Message.warning('任务开始时间不能早于当前时间，且必须在发布日期范围内');
}
function releaseDateIsValid() {
  if (planForm.dateRange.length !== 2) return false;
  return new Date(`${planForm.dateRange[0]}T00:00:00`).getTime() > minimumReleaseTime();
}
function validateReleaseDateSelection() {
  if (!planForm.dateRange.length) return;
  if (!releaseDateIsValid()) {
    planForm.dateRange = [];
    planForm.taskStartTime = '';
    Message.warning('为确保流程正常进行，发布日期必须大于当前时间5天');
    return;
  }
  if (!planForm.taskStartTime) planForm.taskStartTime = `${planForm.dateRange[0]} 09:00:00`;
}
async function uploadStoryboardSample(index: number, files: any[]) {
  const storyboard = storyboards.value[index];
  const file = files?.[0]?.file || files?.[0]?.originFile || files?.[0];
  if (!storyboard || !(file instanceof File)) return;
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
    if (storyboard.sampleAspect && storyboard.sampleAspect !== aspect) {
      Message.warning(`当前选择${sampleAspectText(storyboard.sampleAspect)}，请上传对应比例的视频`);
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
function nextWizardStep() {
  if (wizardStep.value === 1 && !planForm.name) {
    Message.warning('请填写计划名称');
    return;
  }
  if (wizardStep.value === 2) {
    if (!planForm.dateRange.length || !releaseDateIsValid() || !taskStartIsValid() || !planForm.platforms.length) {
      Message.warning('请完善发布日期、任务开始时间和发布平台，且发布日期必须大于当前时间5天');
      return;
    }
    syncStoryboards(planForm.storyboardCount);
  }
  if (wizardStep.value === 3) {
    if (!planForm.description) {
      Message.warning('请填写活动文案');
      return;
    }
    if (planForm.type === '半原创' && storyboards.value.some(item => !item.requirement || !item.script || !item.sampleAspect || !item.sampleVideoUrl)) {
      Message.warning('请完善每个分镜的拍摄要求、台词、画面方向并上传对应样例视频');
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
    Message.warning('请完善活动信息，且发布日期必须大于当前时间5天');
    return false;
  }
  if (!selectedEmployeeIds.value.length) { Message.warning('请至少选择一名员工'); return false; }
  if (planForm.type === '半原创' && storyboards.value.some(item => !item.requirement || !item.script || !item.sampleAspect || !item.sampleVideoUrl)) { Message.warning('请完善每个分镜的拍摄要求、台词、画面方向并上传对应样例视频'); return false; }
  return true;
}
function toLocalDateTime(date: string, end = false) { return `${date}T${end ? '23:59:59' : '00:00:00'}`; }
function buildTaskInstruction() {
  return [
    planForm.description,
    planForm.topic && `发布话题：${planForm.topic}`,
    `发布平台：${planForm.platforms.join('、')}`,
    planForm.type === '原创'
      ? `拍摄要求：${planForm.originalRequirement}`
      : `分镜要求：${storyboards.value.map((item, index) => [
        `${index + 1}.${item.requirement}`,
        `台词：${item.script}`,
        item.sampleVideoUrl && `样例视频：${item.sampleVideoUrl}`,
        item.sampleAspect && `样例比例：${sampleAspectText(item.sampleAspect)}`,
        item.sampleCoverUrl && `样例封面：${item.sampleCoverUrl}`
      ].filter(Boolean).join('；')).join(' | ')}`
  ].filter(Boolean).join('\n').slice(0, 1000);
}
async function persistPlan(publish: boolean) {
  const [startDate, endDate] = planForm.dateRange;
  const taskInstruction = buildTaskInstruction();
  if (editingActivityId.value) {
    await updateContentActivity(editingActivityId.value, {
      name: planForm.name,
      objective: planForm.description,
      startTime: planForm.taskStartTime.replace(' ', 'T'),
      endTime: toLocalDateTime(endDate, true),
      taskInstruction,
      creationMode: planForm.type === '原创' ? 'SELF_CREATED' : 'AI_ASSISTED',
      storyboardCount: planForm.type === '原创' ? 1 : planForm.storyboardCount,
      trainingPolicy: 'NONE',
      employeeIds: selectedEmployeeIds.value
    });
    return;
  }
  const activityId = await createContentActivity({
    name: planForm.name, objective: planForm.description,
    startTime: planForm.taskStartTime.replace(' ', 'T'), endTime: toLocalDateTime(endDate, true)
  });
  const planId = await createContentPlan({
    activityId, name: planForm.name, taskInstruction,
    creationMode: planForm.type === '原创' ? 'SELF_CREATED' : 'AI_ASSISTED',
    storyboardCount: planForm.type === '原创' ? 1 : planForm.storyboardCount,
    trainingPolicy: 'NONE', deadline: toLocalDateTime(endDate, true)
  });
  if (publish) await publishContentPlan(planId, selectedEmployeeIds.value, `content-plan-${planId}-${Date.now()}`);
}
async function saveDraft() {
  if (!planForm.name || planForm.dateRange.length !== 2 || !releaseDateIsValid() || !taskStartIsValid() || !planForm.description) {
    Message.warning('请填写计划名称、活动文案，并选择大于当前时间5天的发布日期');
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
    await persistPlan(true);
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
function cancelPlan(record: PlanRow) {
  Modal.warning({
    title: '取消发布计划',
    content: `取消后，计划“${record.name}”及未完成的员工任务将停止执行，是否继续？`,
    hideCancel: false,
    onOk: async () => {
      try {
        await terminateContentActivity(Number(record.id));
        await loadPlans();
        Message.success('计划已取消');
      } catch (error) {
        Message.error(error instanceof Error ? error.message : '计划取消失败');
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
      ? allPlans.value.filter(plan => plan.startDate <= iso && plan.endDate >= iso)
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
const accountFilters = reactive({ keyword: '', type: '', status: '', employee: '' });
type AccountRow = {
  id: number; index: number; platform: string; platformShort: string; platformClass: string;
  name: string; accountId: string; type: string; org: string; employee: string;
  employeeId: number; organizationId?: number; updated: string; status: string; rawStatus: ContentAccountItem['status'];
};
const accountColumns: TableColumnData[] = [
  { title: '序号', dataIndex: 'index', width: 80 }, { title: '平台', slotName: 'platform', width: 130 },
  { title: '账号名称', dataIndex: 'name', width: 180 }, { title: '平台账号ID', dataIndex: 'accountId', width: 180 },
  { title: '账号类型', dataIndex: 'type' }, { title: '归属组织', dataIndex: 'org', width: 220 },
  { title: '归属员工', dataIndex: 'employee' }, { title: '更新时间', dataIndex: 'updated', width: 170 },
  { title: '账号状态', slotName: 'status' }, { title: '操作', slotName: 'actions', fixed: 'right' }
];
const accounts = ref<AccountRow[]>([]);
const accountsLoading = ref(false);
const selectedAccountIds = ref<number[]>([]);
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
    platformClass: platformMeta.className, name: item.accountName, accountId: item.platformAccountId,
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
const accountFormRef = ref<any>();
const accountRules = {
  platform: [{ required: true, message: '请选择发布平台' }],
  accountType: [{ required: true, message: '请选择账号类型' }],
  accountName: [{ required: true, message: '请输入账号名称' }, { minLength: 2, message: '账号名称至少2个字符' }],
  platformAccountId: [
    { required: true, message: '请输入平台账号ID' },
    { match: /^[A-Za-z0-9_.@-]+$/, message: '平台账号ID仅支持字母、数字及 . _ - @' }
  ],
  employeeId: [{ required: true, type: 'number' as const, min: 1, message: '请选择归属员工' }]
};
type AccountFormState = Omit<ContentAccountPayload, 'employeeId'> & { employeeId?: number };
const accountForm = reactive<AccountFormState>({ platform: '抖音', accountName: '', platformAccountId: '', accountType: '职人', employeeId: undefined });
async function openAccountEditor(record?: AccountRow, readonly = false) {
  await loadEmployees('account');
  accountEditingId.value = record?.id;
  accountReadonly.value = readonly;
  Object.assign(accountForm, record ? {
    platform: record.platform, accountName: record.name, platformAccountId: record.accountId,
    accountType: record.type, organizationId: record.organizationId,
    employeeId: record.employeeId
  } : { platform: '抖音', accountName: '', platformAccountId: '', accountType: '职人', organizationId: undefined, employeeId: undefined });
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
const importVisible = ref(false);
const accountImporting = ref(false);
const importFile = ref<File>();
async function openImport() { await loadEmployees('account'); importFile.value = undefined; importVisible.value = true; }
function downloadAccountTemplate() { downloadCsv('账号导入模板.csv', [['平台', '账号名称', '平台账号ID', '账号类型', '员工工号'], ['抖音', '示例账号', 'DY000001', '职人', '100001']]); }
function handleImportFile(files: any[]) { importFile.value = files?.[0]?.file || files?.[0]?.originFile || files?.[0]; }
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
function handlePersonnelFile(files: any[]) {
  personnelImportFile.value = files?.[0]?.file || files?.[0]?.originFile || files?.[0];
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
</script>

<style scoped>
.video-center { min-width:1080px; padding:16px 20px 24px; box-sizing:border-box; color:var(--tql-text-primary, #1d2129); }
.module-content { display:grid; min-width:0; gap:16px; }
.search-panel, .data-panel, .calendar-panel, .trend-panel { width:100%; min-width:0; box-sizing:border-box; overflow:hidden; border:1px solid #e5e6eb; border-radius:8px; box-shadow:none; }
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
  flex:0 0 230px !important;
  width:230px !important;
  min-width:230px !important;
  max-width:230px !important;
}
.search-panel .plan-search-form :deep(.plan-date-item .arco-picker) { width:320px; }
.search-panel > :deep(.arco-card-body) > .search-actions {
  align-self:stretch;
  margin-left:auto;
}
.search-actions { display:flex; flex:0 0 auto; gap:8px; padding-left:16px; border-left:1px solid #e5e6eb; }
.panel-toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px; }
.panel-toolbar > div { display:flex; align-items:center; gap:10px; }
.panel-toolbar strong { font-size:16px; }
.panel-toolbar span { color:#86909c; font-size:12px; }
.status-dot { display:inline-flex; align-items:center; gap:7px; white-space:nowrap; }
.status-dot::before { width:7px; height:7px; border-radius:50%; content:""; }
.status-active::before { background:#00b42a; box-shadow:0 0 0 3px #e8ffea; }
.status-draft::before { background:#ff7d00; box-shadow:0 0 0 3px #fff7e8; }
.status-terminated::before { background:#f53f3f; box-shadow:0 0 0 3px #ffece8; }
.status-done::before { background:#86909c; box-shadow:0 0 0 3px #f2f3f5; }
.progress-cell { display:flex; align-items:center; gap:10px; }
.progress-cell :deep(.arco-progress) { width:105px; }
.progress-cell span { color:#4e5969; font-size:12px; }
small { color:#86909c; }
.empty-state { display:flex; min-height:320px; flex-direction:column; align-items:center; justify-content:center; }
.empty-illustration { display:grid; width:62px; height:62px; place-items:center; margin-bottom:14px; color:#165dff; background:#e8f3ff; border-radius:50%; font-size:28px; }
.empty-state strong { font-size:16px; }.empty-state span { margin:8px 0 18px; color:#86909c; }
.calendar-toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:20px; }
.month-switch, .legend { display:flex; align-items:center; gap:12px; }.month-switch strong { min-width:120px; text-align:center; font-size:18px; }
.legend span { display:flex; align-items:center; gap:6px; color:#4e5969; font-size:12px; }.legend i { width:9px; height:9px; border-radius:50%; }
.legend-create { border:1px dashed #165dff; }.legend-draft { background:#ff7d00; }.legend-active { background:#165dff; }.legend-terminated { background:#f53f3f; }.legend-completed { background:#00b42a; }.legend-expired { background:#c9cdd4; }
.weekday-row, .calendar-grid { display:grid; grid-template-columns:repeat(7,1fr); }
.weekday-row { padding:10px 0; color:#86909c; background:#f7f8fa; text-align:center; font-size:12px; }
.calendar-day { min-height:145px; padding:12px; border-right:1px solid #e5e6eb; border-bottom:1px solid #e5e6eb; background:#fff; }
.calendar-day:nth-child(7n+1) { border-left:1px solid #e5e6eb; }.calendar-day.muted { background:#fafafa; color:#c9cdd4; }.calendar-day.today { box-shadow:inset 0 0 0 2px #165dff; }
.day-head { display:flex; align-items:center; justify-content:space-between; }.day-head strong { font-size:15px; }.day-head span { padding:2px 6px; color:#86909c; background:#f2f3f5; border-radius:10px; font-size:10px; }
.create-day { width:100%; height:86px; margin-top:10px; color:#165dff; background:transparent; border:1px dashed #bedaff; border-radius:6px; cursor:pointer; opacity:0; }.calendar-day:hover .create-day { opacity:1; }
.day-tasks { display:grid; gap:6px; margin-top:8px; }.day-tasks button { display:grid; gap:5px; padding:8px; border:1px solid transparent; border-left-width:3px; border-radius:4px; text-align:left; cursor:pointer; transition:box-shadow .2s ease, transform .2s ease; }.day-tasks button:hover { box-shadow:0 2px 8px rgba(0,0,0,.08); transform:translateY(-1px); }.day-tasks button:disabled { cursor:default; opacity:.8; transform:none; }.task-meta { display:flex; align-items:center; justify-content:space-between; gap:6px; min-width:0; }.task-meta > span { overflow:hidden; font-size:10px; text-overflow:ellipsis; white-space:nowrap; }.task-meta :deep(.arco-tag) { flex:none; height:20px; padding:0 6px; border-radius:10px; line-height:18px; }.day-tasks strong { overflow:hidden; color:#1d2129; font-size:12px; text-overflow:ellipsis; white-space:nowrap; }
.calendar-task-progress { display:grid; grid-template-columns:minmax(28px,1fr) auto; align-items:center; gap:6px; min-width:0; }
.calendar-task-progress > span { color:#4e5969; font-size:10px; line-height:12px; white-space:nowrap; }
.calendar-task-progress :deep(.arco-progress) { min-width:0; }
.task-active { border-color:#bedaff !important; border-left-color:#165dff !important; background:#e8f3ff; }.task-active .task-meta > span { color:#165dff; }
.task-draft { border-color:#ffe4ba !important; border-left-color:#ff7d00 !important; background:#fff7e8; }.task-draft .task-meta > span { color:#d25f00; }
.task-terminated { border-color:#fdcdc5 !important; border-left-color:#f53f3f !important; background:#fff3f0; }.task-terminated .task-meta > span { color:#cb2634; }.task-terminated :deep(.arco-progress-line-bar) { background:#f53f3f !important; }
.task-completed { border-color:#aff0b5 !important; border-left-color:#00b42a !important; background:#e8ffea; }.task-completed .task-meta > span { color:#009a29; }
.task-expired { border-color:#e5e6eb !important; border-left-color:#86909c !important; background:#f7f8fa; }.task-expired .task-meta > span { color:#86909c; }
.platform-tabs { display:flex; align-items:center; gap:10px; }.platform-tabs button { display:flex; align-items:center; gap:7px; padding:8px 15px; color:#4e5969; background:#fff; border:1px solid #e5e6eb; border-radius:20px; cursor:pointer; }.platform-tabs button.active { color:#165dff; border-color:#165dff; box-shadow:0 0 0 2px #e8f3ff; }
.platform-mark, .account-platform i { display:inline-grid; width:22px; height:22px; place-items:center; color:#fff; border-radius:6px; font-style:normal; font-size:11px; }.all { background:#165dff; }.douyin { background:#111; }.kuaishou { background:#ff5c38; }.redbook { background:#f53f3f; }.channels { background:#f7a928; }
.metric-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:16px; }.metric-grid article { padding:24px 28px; border:1px solid #e5e6eb; border-radius:8px; background:#fff; overflow:hidden; position:relative; }.metric-grid article::after { position:absolute; right:-20px; bottom:-40px; width:130px; height:130px; border-radius:50%; opacity:.35; content:""; }
.metric-grid span { display:block; color:#4e5969; }.metric-grid strong { display:block; margin:8px 0; font-size:32px; }.metric-grid small { color:#00b42a; }.metric-blue::after { background:#bedaff; }.metric-blue strong { color:#165dff; }.metric-purple::after { background:#e8d9ff; }.metric-purple strong { color:#722ed1; }.metric-green::after { background:#b5f5ec; }.metric-green strong { color:#0f9f85; }
.panel-title { display:flex; align-items:center; justify-content:space-between; }.panel-title > div { display:flex; flex-direction:column; gap:5px; }.panel-title strong { font-size:16px; }.panel-title span { color:#86909c; font-size:12px; }
.chart { display:grid; grid-template-columns:45px 1fr; margin-top:24px; }.y-axis { display:flex; flex-direction:column; justify-content:space-between; height:280px; color:#86909c; font-size:11px; }.chart svg { width:100%; height:280px; overflow:visible; }.grid-lines line { stroke:#e5e6eb; stroke-width:1; }.x-axis { grid-column:2; display:flex; justify-content:space-between; padding-top:10px; color:#86909c; font-size:11px; }
.platform-tabs.inner { padding-bottom:16px; border-bottom:1px solid #e5e6eb; }.platform-actions { display:flex; gap:10px; margin-left:auto; }
.account-platform { display:flex; align-items:center; gap:8px; }.account-platform i { width:24px; height:24px; }
.inline-wizard-page { min-height:calc(100vh - 100px); background:#fff; border:1px solid #e5e6eb; border-radius:8px; }
.inline-wizard-header { display:flex; height:54px; align-items:center; gap:4px; padding:0 12px; border-bottom:1px solid #e5e6eb; }
.inline-wizard-header strong { font-size:15px; font-weight:500; }
.wizard-back { color:#4e5969; }
.wizard-shell { display:flex; width:100%; height:calc(100vh - 155px); min-width:0; min-height:0; overflow:hidden; flex-direction:column; background:#fff; }
.wizard-progress { flex:none; padding:64px 40px 42px; background:#fff; }
.wizard-progress :deep(.arco-steps) { max-width:760px; margin:0 auto; }
.wizard-content { flex:1; min-width:0; min-height:0; overflow-x:hidden; overflow-y:auto; width:min(760px, calc(100% - 64px)); box-sizing:border-box; margin:0 auto; padding:8px 0 48px; scrollbar-width:none; -ms-overflow-style:none; }
.wizard-content::-webkit-scrollbar { display:none; width:0; height:0; }
.wizard-card { width:100%; min-width:0; min-height:0; overflow-x:hidden; padding:0; background:#fff; border:0; border-radius:0; }
.wizard-card :deep(.arco-row),.wizard-card :deep(.arco-col) { min-width:0; max-width:100%; }
.wizard-card :deep(.arco-form-item) { margin-bottom:24px; }
.mode-options { display:grid; grid-template-columns:repeat(2,1fr); gap:18px; }
.mode-options button { display:grid; grid-template-columns:36px 1fr; gap:3px 12px; padding:22px; color:#4e5969; background:#fff; border:1px solid #e5e6eb; border-radius:8px; text-align:left; cursor:pointer; transition:.2s; }
.mode-options button svg { grid-row:1/3; align-self:center; color:#86909c; font-size:28px; }
.mode-options button strong { color:#1d2129; font-size:16px; }
.mode-options button span { color:#86909c; font-size:12px; line-height:20px; }
.mode-options button:hover { border-color:#94bfff; }.mode-options button.selected { background:#f2f7ff; border-color:#165dff; box-shadow:0 0 0 2px #e8f3ff; }.mode-options button.selected svg,.mode-options button.selected strong { color:#165dff; }
.platform-checks { display:flex; min-height:40px; align-items:center; gap:18px; }
.delivery-period { display:flex; flex-direction:column; gap:2px; line-height:20px; }
.delivery-period span { display:block; white-space:nowrap; }
.topic-field { display:flex; width:100%; flex-direction:column; align-items:flex-start; }
.topic-editor { display:flex; width:100%; max-width:608px; gap:12px; }
.topic-editor :deep(.arco-input-wrapper) { flex:1; }
.topic-list { display:flex; flex-wrap:wrap; gap:8px; margin-top:12px; }
.topic-list :deep(.arco-tag) { font-size:13px; }
.topic-tip { margin-top:8px; color:#86909c; font-size:12px; line-height:20px; }
.storyboard-toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:20px; }
.storyboard-toolbar > div { display:flex; flex-direction:column; gap:5px; }.storyboard-toolbar strong { font-size:16px; }.storyboard-toolbar span { color:#86909c; font-size:12px; }
.storyboard-toolbar .storyboard-actions { flex-direction:row; align-items:center; gap:12px; }
.storyboard-toolbar .storyboard-actions > span { flex:none; color:#4e5969; white-space:nowrap; }
.storyboard-content-form { margin-bottom:24px; padding-bottom:4px; border-bottom:1px solid #e5e6eb; }
.storyboard-editor-grid { width:100%; }
.storyboard-copy-column,.storyboard-media-column { min-width:0; }
.storyboard-media-column :deep(.arco-upload-wrapper),.storyboard-media-column :deep(.arco-upload) { display:block; width:100%; }
.storyboard-media-column :deep(.arco-upload-list) { width:100%; }
.aspect-options { display:flex; width:100%; }
.aspect-options :deep(.arco-radio-button) { flex:1; text-align:center; }
.video-upload.large { box-sizing:border-box; margin:0 auto; transition:width .2s ease, height .2s ease; }
.video-upload.large.landscape { width:100%; height:auto; aspect-ratio:16/9; }
.video-upload.large.portrait { width:min(72%, 250px); height:auto; aspect-ratio:9/16; }
.original-requirement { margin-top:24px; }
.confirm-summary { display:grid; grid-template-columns:repeat(3,1fr); margin-bottom:24px; border-top:1px solid #e5e6eb; border-left:1px solid #e5e6eb; }
.confirm-summary > div { min-height:88px; padding:18px 20px; border-right:1px solid #e5e6eb; border-bottom:1px solid #e5e6eb; }
.confirm-summary span,.confirm-summary strong { display:block; }.confirm-summary span { margin-bottom:8px; color:#86909c; font-size:12px; }.confirm-summary strong { font-size:15px; }
.confirm-copy { margin-top:22px; padding:20px; background:#f7f8fa; border-radius:6px; }.confirm-copy p { margin:6px 0 18px; color:#4e5969; line-height:1.7; }.confirm-copy p:last-child { margin-bottom:0; }
.wizard-footer { flex:none; width:100%; box-sizing:border-box; padding:16px 32px; background:#fff; border-top:1px solid #e5e6eb; box-shadow:0 -2px 8px rgba(0,0,0,.03); }
.wizard-footer-actions { display:flex; width:100%; min-width:0; min-height:32px; align-items:center; justify-content:flex-end; gap:8px; margin:0; }
.wizard-footer-actions > .arco-space { margin-left:0; }
.form-section { padding:20px 2px 26px; border-bottom:1px solid #e5e6eb; }.form-section:first-child { padding-top:0; }.form-section h3 { display:flex; align-items:center; gap:9px; margin:0 0 20px; font-size:16px; }.form-section h3 span { display:grid; width:24px; height:24px; place-items:center; color:#fff; background:#165dff; border-radius:50%; font-size:12px; }
.section-title-row { display:flex; align-items:center; justify-content:space-between; }.full-width { width:100%; }.release-date-item :deep(.arco-form-item-extra) { color:#f53f3f; }.account-selector { display:flex; align-items:center; justify-content:flex-start; gap:18px; width:100%; padding:12px 16px; background:#f7f8fa; border-radius:6px; }.account-selector > span { margin-left:auto; color:#4e5969; }.account-selector strong { color:#165dff; }
.duration-tip { margin-top:6px; color:#86909c; font-size:12px; }.video-upload { display:flex; width:170px; height:230px; flex-direction:column; align-items:center; justify-content:center; gap:8px; color:#86909c; background:#f7f8fa; border:1px dashed #c9cdd4; border-radius:8px; }.video-upload svg { color:#165dff; font-size:28px; }.video-upload strong { color:#4e5969; }.video-upload span { font-size:11px; }
.drawer-footer { display:flex; align-items:center; justify-content:space-between; width:100%; }.drawer-footer > span { color:#86909c; font-size:12px; }
.detail-hero { display:flex; align-items:flex-start; justify-content:space-between; padding:4px 0 22px; }.detail-hero h2 { margin:12px 0 6px; }.detail-hero p { margin:0; color:#86909c; }.detail-page > h3 { margin:26px 0 12px; }.detail-copy { padding:16px; color:#4e5969; line-height:1.8; background:#f7f8fa; border-radius:6px; }
.delivery-summary { display:flex; align-items:center; gap:24px; margin-bottom:16px; padding:12px 16px; background:#f7f8fa; }.delivery-summary button { margin-left:auto; }
.employee-search { border-radius:8px; background:#f7f8fa; }
.org-selector { display:grid; grid-template-columns:minmax(0,1.75fr) minmax(300px,.85fr); height:520px; margin-top:16px; overflow:hidden; border:1px solid #e5e6eb; border-radius:10px; background:#fff; }
.org-tree,.selected-org { min-width:0; padding:20px; overflow:hidden; }
.org-tree { border-right:1px solid #e5e6eb; }
.selected-org { display:flex; flex-direction:column; background:linear-gradient(180deg,#f7f9ff 0%,#f7f8fa 100%); }
.selector-heading { display:flex; align-items:center; justify-content:space-between; height:28px; margin-bottom:14px; }
.selector-heading strong { color:#1d2129; font-size:15px; font-weight:600; }
.selector-heading span { padding:3px 9px; color:#165dff; font-size:12px; background:#e8f3ff; border-radius:10px; }
.employee-tree-loading { display:block; height:420px; overflow:auto; padding-right:4px; }
.selected-employee-list { display:grid; gap:10px; padding-right:4px; overflow:auto; }
.selected-employee { position:relative; padding:13px 34px 12px 14px; border:1px solid #d9e5ff; border-radius:8px; background:#fff; box-shadow:0 2px 8px rgba(22,93,255,.06); }
.selected-employee span,.selected-employee small { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.selected-employee span { color:#1d2129; font-size:14px; font-weight:600; }
.selected-employee-meta { display:grid; gap:4px; margin-top:7px; }
.selected-employee small { color:#6b7785; font-size:12px; }
.selected-employee button { position:absolute; top:12px; right:10px; width:22px; height:22px; padding:0; color:#86909c; background:#f2f3f5; border:0; border-radius:50%; cursor:pointer; }
.selected-employee button:hover { color:#f53f3f; background:#ffece8; }
.selection-tip { margin:auto 0 0; padding-top:14px; color:#86909c; font-size:12px; line-height:1.6; }
.personnel-import-toolbar { display:flex; align-items:center; gap:12px; margin:16px 0; padding:14px 16px; background:#f7f8fa; border-radius:8px; }
.personnel-file-name { min-width:0; flex:1; overflow:hidden; color:#4e5969; text-overflow:ellipsis; white-space:nowrap; }
.personnel-import-footer { display:flex; align-items:center; justify-content:space-between; margin-top:16px; padding-top:16px; color:#4e5969; border-top:1px solid #e5e6eb; }
.selected-count { display:inline-flex; align-items:center; gap:5px; color:#4e5969; white-space:nowrap; }.selected-count strong { color:#165dff; }.selected-count :deep(.arco-link) { margin-left:5px; }
.selected-personnel-alert { margin-bottom:16px; }
.account-form-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:0 20px; }
.account-status-tip { align-self:start; margin-top:30px; }
.import-drop { display:flex; height:180px; flex-direction:column; align-items:center; justify-content:center; gap:10px; }.import-drop svg { color:#165dff; font-size:32px; }.import-drop span { color:#86909c; font-size:12px; }
@media (max-width:1280px) { .search-panel form { grid-template-columns:repeat(2,minmax(240px,1fr)); } }
</style>
