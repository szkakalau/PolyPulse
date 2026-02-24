const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8000"

export async function fetchWhales() {
  const res = await fetch(`${BASE_URL}/api/whales`)
  if (!res.ok) throw new Error("Failed to load whales")
  return res.json()
}

export async function fetchSmartWallets() {
  const res = await fetch(`${BASE_URL}/api/smart`)
  if (!res.ok) throw new Error("Failed to load smart wallets")
  return res.json()
}

export async function fetchTrades() {
  const res = await fetch(`${BASE_URL}/api/trades`)
  if (!res.ok) throw new Error("Failed to load trades")
  return res.json()
}

export async function refreshData() {
  const res = await fetch(`${BASE_URL}/api/refresh`, { method: "POST" })
  if (!res.ok) throw new Error("Failed to refresh data")
  return res.json()
}
