const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8000"

function buildQuery(params) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      query.set(key, String(value))
    }
  })
  const queryString = query.toString()
  return queryString ? `?${queryString}` : ""
}

export async function fetchWhales({ limit = 50, offset = 0, sort = "latest" } = {}) {
  const res = await fetch(`${BASE_URL}/api/whales${buildQuery({ limit, offset, sort })}`)
  if (!res.ok) throw new Error("Failed to load whales")
  return res.json()
}

export async function fetchSmartWallets({ limit = 50, offset = 0 } = {}) {
  const res = await fetch(`${BASE_URL}/api/smart${buildQuery({ limit, offset })}`)
  if (!res.ok) throw new Error("Failed to load smart wallets")
  return res.json()
}

export async function fetchTrades({ limit = 100, offset = 0 } = {}) {
  const res = await fetch(`${BASE_URL}/api/trades${buildQuery({ limit, offset })}`)
  if (!res.ok) throw new Error("Failed to load trades")
  return res.json()
}

export async function refreshData() {
  const res = await fetch(`${BASE_URL}/api/refresh`, { method: "POST" })
  if (!res.ok) throw new Error("Failed to refresh data")
  return res.json()
}

export async function fetchSignalCredibility() {
  const res = await fetch(`${BASE_URL}/insights/credibility`)
  if (!res.ok) throw new Error("Failed to load credibility")
  return res.json()
}

export async function fetchDeliveryObservability() {
  const res = await fetch(`${BASE_URL}/insights/delivery`)
  if (!res.ok) throw new Error("Failed to load delivery")
  return res.json()
}

export async function fetchSignalDetail(signalId) {
  const res = await fetch(`${BASE_URL}/signals/${signalId}`)
  if (!res.ok) throw new Error("Failed to load signal")
  return res.json()
}
