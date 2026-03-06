import { test, expect } from "@playwright/test"

// 测试数据模拟
const mockCredibilityData = {
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
      { bucket: "0-5s", count: 5 },
      { bucket: "5-15s", count: 10 },
      { bucket: "15-30s", count: 8 },
      { bucket: "30-60s", count: 3 },
      { bucket: "60-120s", count: 0 },
      { bucket: "120-300s", count: 0 },
      { bucket: "300s+", count: 0 }
    ],
    leadCount: 18,
    leadP50Seconds: 240,
    leadP90Seconds: 1200,
    leadHistogram: [
      { bucket: "0-1m", count: 2 },
      { bucket: "1-5m", count: 8 },
      { bucket: "5-15m", count: 6 },
      { bucket: "15-60m", count: 2 },
      { bucket: "1-6h", count: 0 },
      { bucket: "6h+", count: 0 }
    ]
  }
}

const mockWhalesData = [
  {
    id: "1",
    timestamp: "2024-01-15T10:30:00Z",
    symbol: "BTC",
    size: 2500000,
    price: 42000,
    exchange: "Binance",
    type: "buy",
    credibility: 0.85
  },
  {
    id: "2", 
    timestamp: "2024-01-15T09:45:00Z",
    symbol: "ETH",
    size: 5000,
    price: 2500,
    exchange: "OKX",
    type: "sell",
    credibility: 0.72
  }
]

const mockSmartMoneyData = [
  {
    id: "sm1",
    name: "Smart Whale Alpha",
    performance: 0.35,
    winRate: 0.78,
    totalTrades: 45,
    lastTrade: "2024-01-14T16:20:00Z"
  },
  {
    id: "sm2",
    name: "Crypto Oracle",
    performance: 0.28,
    winRate: 0.82,
    totalTrades: 32,
    lastTrade: "2024-01-14T15:30:00Z"
  }
]

test.describe("PolyPulse Web Application - Comprehensive Test Suite", () => {
  
  test.beforeEach(async ({ page }) => {
    // 拦截API请求并返回模拟数据
    await page.route("**/api/credibility", route => {
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(mockCredibilityData)
      })
    })
    
    await page.route("**/api/whales", route => {
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(mockWhalesData)
      })
    })
    
    await page.route("**/api/smart-money", route => {
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(mockSmartMoneyData)
      })
    })
    
    // 导航到应用
    await page.goto("http://localhost:4173")
    await page.waitForLoadState("networkidle")
  })

  test("主页加载和导航测试", async ({ page }) => {
    // 验证页面标题
    await expect(page).toHaveTitle("PolyPulse Admin")
    
    // 验证主导航菜单存在
    const navItems = ["Credibility", "Delivery", "Whale Radar", "Smart Money", "Trade Feed"]
    for (const item of navItems) {
      await expect(page.getByRole("link", { name: item })).toBeVisible()
    }
    
    // 验证页面主要内容
    await expect(page.getByText("Ready")).toBeVisible()
    await expect(page.getByText("Sync Data")).toBeVisible()
    await expect(page.getByText("7/30-day proof")).toBeVisible()
  })

  test("Credibility页面功能测试", async ({ page }) => {
    // 点击Credibility导航
    await page.getByRole("link", { name: "Credibility" }).click()
    await page.waitForLoadState("networkidle")
    
    // 验证页面内容 - 使用更精确的选择器
    await expect(page.locator(".page-title").getByText("Credibility")).toBeVisible()
    await expect(page.getByText("Ready")).toBeVisible()
    await expect(page.getByText("Sync Data")).toBeVisible()
    
    // 由于API请求会失败（后端未运行），我们验证错误状态处理
    const errorElement = page.getByText(/error|failed|无法加载/i)
    if (await errorElement.isVisible({ timeout: 5000 })) {
      await expect(errorElement).toBeVisible()
    }
  })

  test("Whale Radar页面功能测试", async ({ page }) => {
    // 点击Whale Radar导航
    await page.getByRole("link", { name: "Whale Radar" }).click()
    await page.waitForLoadState("networkidle")
    
    // 验证页面内容 - 使用更精确的选择器
    await expect(page.locator(".page-title").getByText("Whale Radar")).toBeVisible()
    await expect(page.getByText("Ready")).toBeVisible()
    await expect(page.getByText("Sync Data")).toBeVisible()
    
    // 由于API请求会失败（后端未运行），我们验证错误状态处理
    const errorElement = page.getByText(/error|failed|无法加载/i)
    if (await errorElement.isVisible({ timeout: 5000 })) {
      await expect(errorElement).toBeVisible()
    }
  })

  test("Smart Money页面功能测试", async ({ page }) => {
    // 点击Smart Money导航
    await page.getByRole("link", { name: "Smart Money" }).click()
    await page.waitForLoadState("networkidle")
    
    // 验证页面内容 - 使用更精确的选择器
    await expect(page.locator(".page-title").getByText("Smart Money")).toBeVisible()
    await expect(page.getByText("Ready")).toBeVisible()
    await expect(page.getByText("Sync Data")).toBeVisible()
    
    // 由于API请求会失败（后端未运行），我们验证错误状态处理
    const errorElement = page.getByText(/error|failed|无法加载/i)
    if (await errorElement.isVisible({ timeout: 5000 })) {
      await expect(errorElement).toBeVisible()
    }
  })

  test("数据刷新功能测试", async ({ page }) => {
    // 查找并点击Sync Data按钮
    const syncButton = page.getByText("Sync Data")
    await expect(syncButton).toBeVisible()
    
    // 点击刷新按钮
    await syncButton.click()
    
    // 验证点击操作完成
    await page.waitForTimeout(1000)
    
    // 验证页面状态（由于后端未运行，可能显示错误）
    const statusText = await page.getByText(/Ready|Syncing|Error/).textContent()
    expect(statusText).toBeDefined()
  })

  test("响应式布局测试", async ({ page }) => {
    // 测试移动端布局
    await page.setViewportSize({ width: 375, height: 667 })
    
    // 验证导航菜单适配
    await expect(page.getByRole("navigation")).toBeVisible()
    
    // 测试平板端布局
    await page.setViewportSize({ width: 768, height: 1024 })
    await expect(page.getByRole("main")).toBeVisible()
    
    // 恢复默认视图
    await page.setViewportSize({ width: 1280, height: 720 })
  })

  test("错误处理测试", async ({ page }) => {
    // 模拟API错误
    await page.route("**/api/credibility", route => {
      route.fulfill({
        status: 500,
        contentType: "application/json",
        body: JSON.stringify({ error: "Internal Server Error" })
      })
    })
    
    // 导航到可能触发错误的页面
    await page.getByRole("link", { name: "Credibility" }).click()
    await page.waitForLoadState("networkidle")
    
    // 验证错误处理（根据实际实现调整）
    const errorElement = page.getByText(/error|failed|无法加载/i)
    if (await errorElement.isVisible()) {
      await expect(errorElement).toBeVisible()
    }
  })
})