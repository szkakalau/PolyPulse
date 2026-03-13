import { test, expect } from "@playwright/test"

test.describe("Debug - Page Structure Analysis", () => {
  
  test("检查页面基本结构", async ({ page }) => {
    await page.goto("/")
    await page.waitForLoadState("networkidle")
    
    // 等待React应用加载
    await page.waitForTimeout(3000)
    
    // 获取页面标题
    const title = await page.title()
    console.log("页面标题:", title)
    
    // 获取body内容
    const bodyContent = await page.content()
    console.log("页面内容长度:", bodyContent.length)
    
    // 检查根元素
    const rootElement = await page.$("#root")
    if (rootElement) {
      const rootHtml = await rootElement.innerHTML()
      console.log("Root元素内容:", rootHtml.substring(0, 500))
    }
    
    // 截图保存
    await page.screenshot({ path: "/tmp/debug-screenshot.png", fullPage: true })
    console.log("截图已保存到 /tmp/debug-screenshot.png")
    
    // 检查所有可见元素
    const allElements = await page.$$("*")
    console.log("页面元素数量:", allElements.length)
    
    // 检查是否有任何文本内容
    const visibleText = await page.evaluate(() => {
      return document.body.innerText
    })
    console.log("可见文本内容:", visibleText.substring(0, 200))
  })
  
  test("检查API端点", async ({ page }) => {
    // 监听网络请求
    page.on("request", request => {
      console.log("请求:", request.method(), request.url())
    })
    
    page.on("response", response => {
      if (response.status() >= 400) {
        console.log("错误响应:", response.status(), response.url())
      }
    })
    
    await page.goto("/")
    await page.waitForLoadState("networkidle")
    await page.waitForTimeout(2000)
  })
})
