import { request } from './request.js'

export function fetchInventoryTasks(storeId) {
	return request({
		url: '/api/cost/inventory-tasks',
		data: { storeId }
	})
}

export function startInventoryTask(taskId) {
	return request({ url: `/api/cost/inventory-tasks/${taskId}/start`, method: 'POST' })
}

export function fetchInventoryItems(taskId) {
	return request({ url: `/api/cost/inventory-tasks/${taskId}/items` })
}

export function submitInventoryCounts(taskId, items, idempotencyKey) {
	return request({
		url: `/api/cost/inventory-tasks/${taskId}/submit`,
		method: 'POST',
		data: { items, idempotencyKey }
	})
}
