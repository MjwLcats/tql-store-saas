import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const source = await readFile(
	new URL('../utils/content-task.js', import.meta.url),
	'utf8'
)
const withResolvedAlias = source.replace(
	'@/utils/date.js',
	new URL('../utils/date.js', import.meta.url).href
)
const moduleUrl = `data:text/javascript;base64,${Buffer.from(withResolvedAlias).toString('base64')}`
const {
	categoryLabel,
	contentCreationLabel,
	contentCreationType,
	formatDeadline,
	stageTone,
	taskDisplayStage,
	taskPlanStatus,
	taskStatusLabel
} = await import(moduleUrl)

test('maps internal stages to stable UI tones', () => {
	assert.equal(stageTone('READY_TO_SHOOT'), 'primary')
	assert.equal(stageTone('LOCKED'), 'warning')
	assert.equal(stageTone('COMPLETED'), 'success')
	assert.equal(stageTone('EXPIRED'), 'danger')
	assert.equal(stageTone('PENDING_REVIEW'), 'muted')
	assert.equal(stageTone('PAUSED'), 'warning')
})

test('plan state takes precedence over the employee execution stage', () => {
	const paused = { planStatus: 'PAUSED', planStatusLabel: '已暂停', stage: 'READY_TO_SHOOT', stageLabel: '待拍摄' }
	assert.equal(taskPlanStatus(paused), 'PAUSED')
	assert.equal(taskDisplayStage(paused), 'PAUSED')
	assert.equal(taskStatusLabel(paused), '已暂停')

	const terminated = { activityStatus: 'TERMINATED', stage: 'READY_TO_SHOOT', stageLabel: '待拍摄' }
	assert.equal(taskDisplayStage(terminated), 'TERMINATED')
	assert.equal(taskStatusLabel(terminated), '已终止')
})

test('formats deadlines in employee-facing language', () => {
	const now = new Date('2026-07-27T10:00:00+08:00')
	assert.match(formatDeadline('2026-07-27T18:00:00+08:00', now), /^今天 /)
	assert.match(formatDeadline('2026-07-26T18:00:00+08:00', now), /^已于 /)
	assert.equal(formatDeadline('invalid', now), '截止时间待确认')
	assert.equal(formatDeadline('', now), '未设置截止时间')
})

test('uses business category labels', () => {
	assert.equal(categoryLabel('ALL'), '全部任务')
	assert.equal(categoryLabel('TODO'), '待完成')
	assert.equal(categoryLabel('PROCESSING'), '处理中')
	assert.equal(categoryLabel('UNKNOWN'), '全部')
})

test('maps backend creation modes to employee-facing video types', () => {
	assert.equal(contentCreationType({ creationMode: 'SELF_CREATED' }), 'ORIGINAL')
	assert.equal(contentCreationLabel({ creationMode: 'SELF_CREATED' }), '原创')
	assert.equal(contentCreationLabel({ creationMode: 'STANDARD_TEMPLATE' }), '半原创')
	assert.equal(contentCreationLabel({ creationMode: 'AI_ASSISTED' }), '半原创')
	assert.equal(contentCreationLabel({ taskInstruction: '拍摄要求：自由拍摄' }), '原创')
})
