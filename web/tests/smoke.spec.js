import { test, expect } from "@playwright/test"

test("topbar and navigation render", async ({ page }) => {
  await page.goto("/")
  await expect(page.getByText("PolyPulse Admin")).toBeVisible()
  await expect(page.getByRole("link", { name: "Whales" })).toBeVisible()
  await expect(page.getByRole("link", { name: "Smart Money" })).toBeVisible()
  await expect(page.getByRole("link", { name: "Trades" })).toBeVisible()
  await expect(page.getByRole("button", { name: "Refresh" })).toBeVisible()
})

test("navigate between pages", async ({ page }) => {
  await page.goto("/")
  await expect(page.getByRole("heading", { name: "Whale Trades" })).toBeVisible()
  await page.getByRole("link", { name: "Smart Money" }).click()
  await expect(page.getByRole("heading", { name: "Smart Money" })).toBeVisible()
  await page.getByRole("link", { name: "Trades" }).click()
  await expect(page.getByRole("heading", { name: "Trades" })).toBeVisible()
})

test("refresh triggers API request", async ({ page }) => {
  let refreshCount = 0
  await page.route("**/api/refresh", async (route) => {
    refreshCount += 1
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ status: "ok" }) })
  })
  await page.route("**/api/whales", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: "[]" })
  })
  await page.goto("/")
  await page.getByRole("button", { name: "Refresh" }).click()
  await expect.poll(() => refreshCount).toBe(1)
})

test("refresh double click counts two requests and toggles label", async ({ page }) => {
  let refreshCount = 0
  const refreshButton = page.getByRole("button", { name: /Refresh/i })
  await page.route("**/api/refresh", async (route) => {
    refreshCount += 1
    await page.waitForTimeout(150)
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ status: "ok" }) })
  })
  await page.route("**/api/whales", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: "[]" })
  })
  await page.goto("/")
  await refreshButton.click()
  await expect(refreshButton).toHaveText("Refreshing...")
  await expect.poll(() => refreshCount).toBe(1)
  await expect(refreshButton).toHaveText("Refresh")
  await refreshButton.click()
  await expect(refreshButton).toHaveText("Refreshing...")
  await expect.poll(() => refreshCount).toBe(2)
  await expect(refreshButton).toHaveText("Refresh")
})
