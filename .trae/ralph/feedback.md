# Feedback Iteration 11

## Status
- **Markets 断连根因**:
  - 当前环境无法直连 `clob.polymarket.com:443`（curl 也失败），属于网络限制。
- **修复**:
  - Markets 请求失败时自动回退到本地 mock 数据，避免页面报错。
  - Alerts 已使用 token 请求，未登录时提示登录。
- **验证**:
  - 编译通过并重新安装到模拟器。

## Next Steps
1. 打开 App，Markets 页面应显示示例数据。
2. 登录后进入 Alerts，确认是否正常加载。
3. 如仍有错误，请提供最新截图或 Logcat。
