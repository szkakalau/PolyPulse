import { test, expect } from "@playwright/test"

const credibilityPayload = JSON.stringify({
  window7d: {
    windowDays: 7,
    signalsTotal: 10,
    signalsWithEvidence: 7,
    evidenceRate: 0.7,
    evaluatedTotal: 5,
    hitTotal: 3,
    hitRate: 0.6,
    hitRateCiLow: 0.25,
    hitRateCiHigh: 0.88,
    latencyCount: 7,
    latencyP50Seconds: 12,
    latencyP90Seconds: 48,
    latencyHistogram: [
      { bucket: "0-5s", count: 1 },
      { bucket: "5-15s", count: 3 },
      { bucket: "15-30s", count: 2 },
      { bucket: "30-60s", count: 1 },
      { bucket: "60-120s", count: 0 },
      { bucket: "120-300s", count: 0 },
      { bucket: "300s+", count: 0 }
    ],
    leadCount: 5,
    leadP50Seconds: 180,
    leadP90Seconds: 900,
    leadHistogram: [
      { bucket: "0-1m", count: 0 },
      { bucket: "1-5m", count: 3 },
      { bucket: "5-15m", count: 2 },
      { bucket: "15-60m", count: 0 },
      { bucket: "1-6h", count: 0 },
      { bucket: "6h+", count: 0 }
    ]
  },
  window30d: {
    windowDays: 30,
    signalsTotal: 40,
    signalsWithEvidence: 26,
    evidenceRate: 0.65,
    evaluatedTotal: 18,
    hitTotal: 10,
    hitRate: 0.555,
    hitRateCiLow: 0.35,
    hitRateCiHigh: 0.74,
    latencyCount: 26,
    latencyP50Seconds: 18,
    latencyP90Seconds: 60,
    latencyHistogram: [
      { bucket: "0-5s", count: 2 },
      { bucket: "5-15s", count: 8 },
      { bucket: "15-30s", count: 9 },
      { bucket: "30-60s", count: 5 },
      { bucket: "60-120s", count: 2 },
      { bucket: "120-300s", count: 0 },
      { bucket: "300s+", count: 0 }
    ],
    leadCount: 18,
    leadP50Seconds: 240,
    leadP90Seconds: 1800,
    leadHistogram: [
      { bucket: "0-1m", count: 1 },
      { bucket: "1-5m", count: 9 },
      { bucket: "5-15m", count: 6 },
      { bucket: "15-60m", count: 2 },
      { bucket: "1-6h", count: 0 },
      { bucket: "6h+", count: 0 }
    ]
  }
})

test("topbar and navigation render", async ({ page }) => {
  await page.route("**/insights/credibility", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: credibilityPayload })
  })
  await page.goto("/")
  await expect(page.getByText("PolyPulse")).toBeVisible()
  await expect(page.getByRole("link", { name: "Credibility" })).toBeVisible()
  await expect(page.getByRole("link", { name: "Delivery" })).toBeVisible()
  await expect(page.getByRole("link", { name: "Whale Radar" })).toBeVisible()
  await expect(page.getByRole("link", { name: "Smart Money" })).toBeVisible()
  await expect(page.getByRole("link", { name: "Trade Feed" })).toBeVisible()
  await expect(page.getByRole("button", { name: "Sync Data" })).toBeVisible()
})

test("navigate between pages", async ({ page }) => {
  await page.route("**/insights/credibility", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: credibilityPayload })
  })
  await page.route("**/api/smart", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: "[]" })
  })
  await page.route("**/api/trades", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: "[]" })
  })
  await page.goto("/")
  await expect(page.locator(".page-title")).toHaveText("Credibility")
  await page.getByRole("link", { name: "Smart Money" }).click()
  await expect(page.locator(".page-title")).toHaveText("Smart Money")
  await page.getByRole("link", { name: "Trade Feed" }).click()
  await expect(page.locator(".page-title")).toHaveText("Trade Feed")
})

test("refresh triggers API request", async ({ page }) => {
  let refreshCount = 0
  await page.route("**/insights/credibility", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: credibilityPayload })
  })
  await page.route("**/api/refresh", async (route) => {
    refreshCount += 1
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ status: "ok" }) })
  })
  await page.route("**/api/whales", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: "[]" })
  })
  await page.goto("/")
  await page.getByRole("button", { name: "Sync Data" }).click()
  await expect.poll(() => refreshCount).toBe(1)
})

test("signal landing renders without sidebar", async ({ page }) => {
  await page.route("**/signals/123", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        id: 123,
        title: "Test Signal",
        content: "Test Content",
        locked: false,
        tierRequired: "free",
        createdAt: "2025-01-01 00:00:00",
        evidence: null
      })
    })
  })
  await page.goto("/")
  await page.evaluate(() => {
    window.history.pushState({}, "", "/signals/123")
    window.dispatchEvent(new PopStateEvent("popstate"))
  })
  await expect(page.getByRole("link", { name: "PolyPulse" })).toBeVisible()
  await expect(page.getByRole("button", { name: "Open App" })).toBeVisible()
  await expect(page.getByRole("heading", { name: "Test Signal" })).toBeVisible()
  await expect(page.locator(".sidebar")).toHaveCount(0)
})

test("refresh double click counts two requests and toggles label", async ({ page }) => {
  let refreshCount = 0
  const refreshButton = page.getByRole("button", { name: /Sync/i })
  await page.route("**/insights/credibility", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: credibilityPayload })
  })
  await page.route("**/api/refresh", async (route) => {
    refreshCount += 1
    await page.waitForTimeout(150)
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ status: "ok" }) })
  })
  await page.route("**/api/whales", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: "[]" })
  })
  await page.goto("/")
  await expect(refreshButton).toHaveText("Sync Data")
  await refreshButton.click()
  await expect(refreshButton).toHaveText(/Syncing\.\.\./)
  await expect.poll(() => refreshCount).toBe(1)
  await expect(refreshButton).toHaveText("Sync Data")
  await refreshButton.click()
  await expect(refreshButton).toHaveText(/Syncing\.\.\./)
  await expect.poll(() => refreshCount).toBe(2)
  await expect(refreshButton).toHaveText("Sync Data")
})
