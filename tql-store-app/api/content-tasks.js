import { request } from './request.js'

export function fetchContentTasks({ category = 'ALL', page = 1, pageSize = 20 } = {}) {
	return request({
		url: '/api/operation/employee/content-tasks',
		data: { category, page, pageSize }
	})
}

export function fetchContentTask(id) {
	return request({
		url: `/api/operation/employee/content-tasks/${encodeURIComponent(id)}`
	})
}

export function startContentTaskCompose(id, data) {
	return request({
		url: `/api/operation/employee/content-tasks/${encodeURIComponent(id)}/compose`,
		method: 'POST',
		data,
		timeout: 30000
	})
}
