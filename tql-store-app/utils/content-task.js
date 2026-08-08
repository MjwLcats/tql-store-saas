import { formatMonthDay, formatTime } from '@/utils/date.js'

const CATEGORY_MAP = Object.freeze({
	ALL: '全部任务',
	TODO: '待完成',
	PROCESSING: '处理中',
	COMPLETED: '已完成',
	EXCEPTION: '异常'
})

export function categoryLabel(category) {
	return CATEGORY_MAP[category] || '全部'
}

export function stageTone(stage) {
	if (stage === 'COMPLETED') return 'success'
	if (stage === 'EXPIRED' || stage === 'TERMINATED') return 'danger'
	if (stage === 'LOCKED' || stage === 'NEEDS_REVISION' || stage === 'PAUSED') return 'warning'
	if (stage === 'PROCESSING' || stage === 'PENDING_REVIEW') return 'muted'
	return 'primary'
}

export function taskPlanStatus(task = {}) {
	return String(task.planStatus || task.activityStatus || task.releaseStatus || task.dispatchStatus || '').toUpperCase()
}

export function taskDisplayStage(task = {}) {
	const planStatus = taskPlanStatus(task)
	if (planStatus === 'PAUSED' || planStatus === 'TERMINATED') return planStatus
	return String(task.stage || '')
}

export function taskStatusLabel(task = {}) {
	const planStatus = taskPlanStatus(task)
	if (planStatus === 'PAUSED') return task.planStatusLabel || '已暂停'
	if (planStatus === 'TERMINATED') return task.planStatusLabel || '已终止'
	return task.stageLabel || '状态更新中'
}

export function contentCreationType(task = {}) {
	const mode = String(task.creationMode || '').toUpperCase()
	if (mode === 'SELF_CREATED') return 'ORIGINAL'
	if (mode === 'STANDARD_TEMPLATE' || mode === 'AI_ASSISTED') return 'SEMI_ORIGINAL'
	const instruction = String(task.taskInstruction || '')
	return /拍摄要求[:：]/.test(instruction) && !/分镜要求[:：]/.test(instruction)
		? 'ORIGINAL'
		: 'SEMI_ORIGINAL'
}

export function contentCreationLabel(task) {
	return contentCreationType(task) === 'ORIGINAL' ? '原创' : '半原创'
}

export function formatDeadline(value, now = new Date()) {
	if (!value) return '未设置截止时间'
	const target = new Date(value)
	if (Number.isNaN(target.getTime())) return '截止时间待确认'
	const diff = target.getTime() - now.getTime()
	const timeText = formatTime(target)
	if (diff < 0) return `已于 ${formatMonthDay(target)} ${timeText} 截止`
	if (diff <= 24 * 60 * 60 * 1000) return `今天 ${timeText} 截止`
	return `${formatMonthDay(target)} ${timeText} 截止`
}
