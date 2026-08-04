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
	if (stage === 'LOCKED' || stage === 'NEEDS_REVISION') return 'warning'
	if (stage === 'PROCESSING' || stage === 'PENDING_REVIEW') return 'muted'
	return 'primary'
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
	const formatter = new Intl.DateTimeFormat('zh-CN', {
		month: '2-digit',
		day: '2-digit',
		hour: '2-digit',
		minute: '2-digit',
		hour12: false
	})
	const diff = target.getTime() - now.getTime()
	if (diff < 0) return `已于 ${formatter.format(target)} 截止`
	if (diff <= 24 * 60 * 60 * 1000) return `今天 ${formatter.format(target).slice(6)} 截止`
	return `${formatter.format(target)} 截止`
}
