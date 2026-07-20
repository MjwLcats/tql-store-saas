import os from 'node:os'

const VIRTUAL_NETWORK_PATTERN = /loopback|vethernet|wsl|docker|vmware|virtualbox|hyper-v|tailscale|zerotier|vpn/i
const PHYSICAL_NETWORK_PATTERN = /wi-?fi|wlan|wireless|ethernet|以太网|无线/i

function isPrivateIPv4(address) {
	const octets = address.split('.').map(Number)
	if (octets.length !== 4 || octets.some((octet) => !Number.isInteger(octet) || octet < 0 || octet > 255)) {
		return false
	}

	return octets[0] === 10
		|| (octets[0] === 172 && octets[1] >= 16 && octets[1] <= 31)
		|| (octets[0] === 192 && octets[1] === 168)
}

function findLanIPv4(networkInterfaces = os.networkInterfaces()) {
	const candidates = Object.entries(networkInterfaces).flatMap(([name, addresses]) =>
		(addresses || [])
			.filter(({ address, family, internal }) =>
				!internal
				&& (family === 'IPv4' || family === 4)
				&& isPrivateIPv4(address)
				&& !VIRTUAL_NETWORK_PATTERN.test(name)
			)
			.map(({ address }) => ({
				address,
				name,
				score: PHYSICAL_NETWORK_PATTERN.test(name) ? 1 : 0
			}))
	)

	candidates.sort((left, right) => right.score - left.score || left.name.localeCompare(right.name))
	return candidates[0]?.address
}

function normalizeApiBaseUrl(value) {
	const normalized = value.trim().replace(/\/$/, '')
	if (!/^https?:\/\/[^/\s]+(?::\d+)?$/i.test(normalized)) {
		throw new Error(`Invalid VITE_API_BASE_URL: ${value}`)
	}
	return normalized
}

export function resolveApiBaseUrl(environment = process.env, networkInterfaces) {
	const configuredUrl = environment.VITE_API_BASE_URL
	if (configuredUrl) return normalizeApiBaseUrl(configuredUrl)

	const host = findLanIPv4(networkInterfaces) || '127.0.0.1'
	return `http://${host}:8080`
}
