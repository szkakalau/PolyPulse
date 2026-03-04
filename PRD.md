# PolyPulse Android 商业化 PRD（优化版）

## 1. 文档信息
- 版本：v1.1
- 日期：2026-03-03
- 适用范围：Android App（主），Backend（支撑），Web（增长落地页）
- 目标读者：产品 / 研发 / 设计 / 增长 / 运营

## 2. 一句话定义
- PolyPulse 是面向预测市场交易者的“可验证信号分发 + 低延迟触达”订阅产品：让用户在关键时刻更早、更稳、更可信地获得行动信息。

## 3. 现状总结（基于当前代码）
### 3.1 已有能力（已上线/已实现）
- Android：Markets、Dashboard（Whale/Smart/Stats）、Signals（锁定/解锁）、Alerts、Profile、Paywall、Trial、Notification Settings、FCM Service（基础）
- Backend：signals、paywall、trial、entitlements、analytics、notifications、watchlist、delivery/credibility insights、feature flags、metrics
- Web：Insights / Delivery / Whales / Smart / Trades（运营看板）；已新增 /signals/:id 分享落地页

### 3.2 当前商业化与可用性缺口
- 价值证明件仍偏“写在 PRD 里”：缺少面向用户的“为什么值得付费”的一屏表达与持续更新机制
- 付费链路缺少“反悔成本降低”：取消/退款规则、订阅异常自助处理、支付失败恢复路径
- Onboarding 缺少默认可用配置：新手进入后仍需要自己理解“该看什么/该开哪些提醒”
- 增长闭环缺少统一入口：分享/SEO/Referral 的入口与 CTA 文案缺少产品化标准

## 4. 用户与场景（更接地气）
### 4.1 核心用户（付费主力）
- 高频交易者：要的是“快”和“少废话”，愿意为领先时间与低噪声付费
- 关键场景：工作/开会时无法盯盘，但愿意在关键时刻被打断；收到推送后 30 秒内会打开 App 看详情

### 4.2 成长用户（转化来源）
- 策略观察者：要的是“可信”与“可复盘”，愿意为“历史表现 + 证据链 + 解释”付费
- 关键场景：每天固定时间刷一次榜单/复盘页，选择是否跟随

### 4.3 新手用户（自然增长入口）
- 新手：要的是“一键可用”，先用上再理解，接受被引导
- 关键场景：第一次安装 2 分钟内必须完成“看到价值 + 启用提醒 + 看到锁定预览”

## 5. 核心闭环（商业化必须跑通）
1) 用户看到价值（可验证）  
2) 用户获得一次强烈“想要”（关键时刻信号/锁定预览/领先时间）  
3) 用户被限制（Free 看不全/延迟/配额）  
4) 用户付费（最少步骤、清晰风险反转）  
5) 用户持续收到价值（低延迟、可靠触达、可复盘）  
6) 用户愿意续费并愿意分享（分享页 + 证据 + CTA）

## 6. 目标与非目标
### 6.1 业务目标（4 周）
- 完成“信号→触达→打开→付费→续费”的最小商业化闭环验证
- 找到 1 个明确、可复制的付费理由（不是“功能多”，而是“更早更稳更可信”）

### 6.2 产品目标（可衡量）
- 北极星指标：有效触达后 5 分钟内的 Signal 打开率（Signal Open / Delivered）
- 付费核心：Trial→Paid 转化率、Paid 续费率、付费用户 7 日留存

### 6.3 非目标（本期不做）
- 自动化下单、交易执行
- 复杂社交（群聊、跟单社区）
- 多端完整一致（先 Android，Web 用于增长/运营）

## 7. 商业化设计（更可落地）
### 7.1 分层与权益（建议以“结果”表达）
| 层级 | 价格建议（人民币） | 用户心智 | 关键权益（结果导向） |
| --- | --- | --- | --- |
| Free | ¥0 | 体验与了解 | 延迟推送（2–5 分钟）、基础看板、有限锁定预览 |
| Pro 月付 | ¥49 | 追求领先与效率 | 低延迟推送（目标 P95 < 30s）、完整信号内容与证据、更多提醒配额 |
| Pro 年付 | ¥399 | 长期使用 | 同 Pro，年付折扣与稳定权益 |
| Elite（后期） | 待定 | 专业深度 | 策略/专家包、定制提醒、优先通道 |

### 7.2 试用与风险反转
- 默认：7 天试用（仅首次）
- 试用期结束前触达：T-48h、T-24h、T-2h 三次（可关闭）
- 明确说明：随时取消，取消后试用到期自动降级，不会额外扣费（按 Google Play 规则表达）

### 7.3 退款/取消/异常处理（必须写进产品）
- 取消：Profile → Subscription → 直达系统订阅页
- 退款：按 Google Play 政策，引导到官方流程 + 产品内 FAQ
- 异常：支付成功未解锁 / 解锁不同步 → 一键“恢复购买” + 后端校验 + 提示预计耗时

### 7.4 付费墙触发策略（更“自然”）
- 主触发：打开 Signal Detail 且 locked
- 次触发：尝试启用 Pro 模板、超过 Free 配额、点击“实时推送”开关
- 频控：同一用户同一会话最多弹 2 次；其余用轻提示条

### 7.5 付费墙内容结构（以可验证为核心）
1) 这条信号能带来什么（领先时间/关键行为）  
2) 为什么可信（证据链：触发来源、触发时刻、市场/钱包、外链）  
3) 你会得到什么（低延迟、更多配额、完整内容）  
4) 价格锚定（年付省多少）  
5) 风险反转（7 天试用、随时取消）  

## 8. 产品体验改造点（更可用）
### 8.1 Onboarding（2 分钟内完成价值体验）
- 第一次打开：三屏内完成
  - 选择关注类目（Politics/Sports/Crypto 等）
  - 一键启用默认提醒模板（可随时关闭）
  - 展示一个“锁定信号预览”并解释：Pro 才能看全 + 低延迟

### 8.2 Signals 体验（付费内容要“像样”）
- 列表：必须可快速判断“是否值得点”
  - 标题 + 触发来源 + 时间 + 锁定态
  - Free 也展示证据摘要（不泄露核心结论，但让人觉得真实）
- 详情：把“证据链”标准化展示；把“下一步动作”表达清楚（不是长文）

### 8.3 Alerts / Push 体验（避免打扰带来的卸载）
- 默认只开 1–2 个高质量模板，避免新手被轰炸
- 允许用户设定“安静时段”
- 提醒到达后落地页必须稳定：点推送打开 Signal Detail，不要迷路

### 8.4 Profile（商业化自助中心）
- 必须能一眼看到：当前层级、到期时间、是否自动续费、入口（管理/恢复购买/FAQ）

## 9. 增长闭环（低 CAC、可复用）
### 9.1 Signal Share Page（已落地，继续产品化）
- URL：/signals/:id（Web）
- 目标：Twitter/Telegram 可读、可转发、可带来注册
- CTA：Open App（Deep Link）+ View Credibility（站内）

### 9.2 Market / Signal SEO Landing（长期）
- Market Landing：/markets/:slug（可渲染 market 概况、成交量、波动、近期 whale）
- Signal Landing：/signals/:id（已实现基础版）
- 内容策略：可索引标题、结构化信息、FAQ、风险提示

### 9.3 Referral（更接地气的规则）
- 邀请者：成功邀请 1 人注册并完成试用激活 → Pro +7 天
- 被邀请者：首月折扣或试用加长（避免双边都无感）

## 10. 指标体系（可执行）
### 10.1 漏斗（必须每天看）
- Landing View → Signup → First Session Activation（启用模板/打开信号）→ Trial Start → Paid → Renewal

### 10.2 产品健康指标
- D1/D7/D30 留存
- Push 送达率、点击率、退订率、卸载率（间接）
- 端到端延迟：Signal Created → Notification Queued → Sent → Opened

### 10.3 商业化指标目标（初版）
- Trial 启动率（在可触达用户中）：> 8%
- Trial→Paid：> 15%
- Paid 续费率：> 60%
- 付费用户 7 日留存：> 40%

## 11. 事件埋点（统一命名与属性）
### 11.1 必埋事件（最小集）
- signup / login
- signal_view（含 locked、signalId、source）
- paywall_view（含 source、message_id 可选）
- trial_started
- subscribe_success / subscribe_failed
- notification_delivered / notification_open（如果系统允许则补充）

### 11.2 A/B 测试方向（只做能解释差异的）
- Paywall 首屏：先“证据”还是先“价格”
- 试用按钮文案：强调“随时取消” vs 强调“领先时间”

## 12. 技术与架构（保留关键、把细节移到附录）
### 12.1 核心原则
- 信号与推送必须解耦（Queue）
- 权限检查必须 server side
- Push 必须 async + 可观测 + 有降级

### 12.2 Deep Link（增长与体验关键）
- 约定：polypulse://signals/{signalId}
- 目标：从分享页/推送点击直接打开对应 Signal Detail

### 12.3 关键接口（对齐现有实现）
- GET /signals（列表，按用户 tier 返回 locked/content）
- GET /signals/{signal_id}（详情，支持 requireUnlocked）
- GET /paywall（方案）
- POST /trial/start（试用）
- GET /entitlements/me（权益）
- POST /analytics/event（埋点）
- GET /insights/credibility（价值证明件）
- GET /insights/delivery（分发可观测）

## 13. 合规与风控（必须产品化）
- 明确免责声明：不构成投资建议
- 风险提示：高波动、可能亏损、请自行判断
- 频控与反滥用：限流、请求体限制、异常行为监控
- 隐私：说明收集哪些数据（设备标识/埋点/推送 token）、如何删除

## 14. 上线与运营（避免“做完功能没人用”）
### 14.1 发布策略
- 灰度：小流量开通付费墙与试用
- 关键开关：Feature flags 控制 paywall/试用/分享入口

### 14.2 客服与自助
- FAQ：订阅没生效、如何取消、如何恢复购买、推送收不到
- 运营策略：试用到期提醒、每日精选（Daily Pulse）、周报复盘

## 15. 里程碑（4 周执行版）
- Week 1：Onboarding + 默认模板 + 锁定预览标准化（确保新手 2 分钟体验到价值）
- Week 2：Paywall 文案/结构 A/B + 恢复购买 + 订阅自助中心
- Week 3：价值证明件前置（Credibility/Delivery 产品化呈现）+ 分享链路完善（Deep Link）
- Week 4：Referral + SEO Landing（先信号页后市场页）+ 漏斗看板与迭代节奏

## 16. Definition of Done（上线“可卖”标准）
- 新用户：2 分钟内完成“启用模板 + 看到锁定预览 + 明白 Pro 价值”
- 付费：从锁定信号到完成订阅 ≤ 60 秒（网络正常）
- 可观测：能回答“我们到底快不快、稳不稳、值不值”（延迟/送达/打开/转化）
- 自助：订阅异常可自助恢复，取消入口清晰

## 17. 可执行任务拆解（Jira 版，按 P0→P1→P2）
### EPIC P0-1：Onboarding 2 分钟激活
- 目标：新用户首次打开 2 分钟内完成“启用模板 + 看到锁定预览”
- 验收标准
  - 首次进入必经引导，不登录也能看到一次锁定预览
  - 一键启用默认模板后，Alerts/Push 设置入口可见且可关闭

### EPIC P0-2：Paywall 与自助订阅中心
- 目标：让用户敢付费、付费后不慌
- 验收标准
  - Paywall 明确写清：试用规则、随时取消、权益差异（Free vs Pro）
  - Profile 显示 tier/到期时间/管理订阅/恢复购买/FAQ
  - 支付成功但未解锁：用户可一键恢复购买并在 10 秒内得到明确结果

### EPIC P0-3：信号“证据链”标准化（让内容更像产品）
- 目标：Free 也能感知真实，Pro 才能看到关键结论
- 验收标准
  - Signal List：每条信号显示触发来源与时间，locked 状态清晰
  - Signal Detail：证据字段标准化展示（sourceType/triggeredAt/marketId/makerAddress/evidenceUrl）
  - Paywall 前置证据摘要但不泄露核心内容

### EPIC P0-4：Deep Link 打通（推送与分享不迷路）
- 目标：从推送/分享页直达对应 Signal
- 验收标准
  - 支持 polypulse://signals/{signalId} 打开 App 并进入 Signal Detail
  - Web 分享页点击 Open App 可尝试打开 App（失败时提示安装/打开方式）

### EPIC P0-5：可观测性最小闭环（每天能复盘）
- 目标：能回答“到底哪里掉链子”
- 验收标准
  - 后端可看到：信号产生、入队、发送、失败、打开（可用时）
  - 漏斗可看：Paywall view → trial start → subscribe success

### EPIC P1-1：安静时段与频控（减少卸载）
- 验收标准
  - 支持设置安静时段，且默认关闭
  - 试用到期提醒与营销推送有频控，用户可关闭

### EPIC P1-2：Referral（可用、可算账）
- 验收标准
  - 邀请链路可追踪，奖励规则清晰且可防刷
  - 运营后台可查看邀请转化与成本

### EPIC P2-1：Market SEO Landing（长期低 CAC）
- 验收标准
  - /markets/:slug 可渲染基础数据与结构化信息
  - 每页包含风险提示与 CTA

## 18. 关键页面信息架构与逐屏文案（可直接交付设计/研发）
### 18.1 Onboarding（3 屏）
#### 屏 1：欢迎与价值主张
- 标题：更早看到关键动作
- 副标题：低延迟推送 + 可验证证据，让你在关键时刻更快做决定
- CTA：开始设置
- 次要 CTA：先逛逛（跳过）

#### 屏 2：选择关注方向（可多选）
- 标题：你更关注哪些市场？
- 选项示例：Politics / Sports / Crypto / Macro / Everything
- 说明：我们会优先推送你关心的信号（可随时更改）
- CTA：下一步

#### 屏 3：一键启用默认模板 + 推送授权
- 标题：开启提醒（建议）
- 模板默认（建议仅 1–2 个）
  - Whale Radar：大额成交（默认开）
  - Daily Pulse：每日精选（默认关）
- 推送授权文案（系统弹窗前）
  - 文案：允许通知，才能在关键时刻第一时间收到信号
  - CTA：允许通知
  - 次要 CTA：暂时不要

### 18.2 Signals 列表（信息密度优先）
- 列表项字段（从左到右/从上到下）
  - 标题（必须短，12–18 字内优先）
  - 触发来源：Whale / Activity / System
  - 时间：xx 分钟前
  - 状态：Locked / Unlocked
- Locked 说明（Free 可见）
  - 文案：预览可见，完整内容需 Pro

### 18.3 Signal 详情（结构化，不写长作文）
#### Unlocked（Pro/Trial）
- 标题：信号标题
- 模块 1：结论（1 行）
  - 例：观察到大额资金集中入场，短期波动概率上升
- 模块 2：行动建议（最多 3 条）
  - 例：关注 xxx 市场；等待价格回撤至 x.xx；设置止损提醒
- 模块 3：证据链（标准字段）
  - Source / Triggered / Market / Wallet / 外链
- 模块 4：风险提示
  - 文案：高波动风险，请自行判断

#### Locked（Free）
- 标题：信号标题
- 模块 1：锁定提示（必须清晰但不冒犯）
  - 文案：这条信号包含行动细节与证据链接，仅 Pro 可查看
- 模块 2：证据摘要（不泄露关键结论）
  - 展示：触发来源、触发时间、市场/钱包（可部分脱敏）
- 模块 3：CTA
  - 主 CTA：解锁 Pro（跳 Paywall，带 source=signal_locked）
  - 次 CTA：先看可信度（跳 Insights/Credibility）

### 18.4 Paywall（可验证结构 + 风险反转）
#### 顶部区（第一屏必须传达“值不值”）
- 标题：解锁低延迟与完整信号
- 三条 bullet（结果导向）
  - 低延迟推送：关键时刻尽早提醒
  - 完整信号内容：结论 + 行动建议 + 证据链接
  - 更多提醒配额：少噪声，更多关键点

#### 证据区（第二屏）
- 标题：为什么可信？
- 文案：我们展示触发来源与可追溯证据，支持复盘
- 展示项（来自 /insights/credibility）
  - 近 7 天命中率（含样本量）
  - 领先时间分布（P50/P90）

#### 价格区（第三屏）
- 默认选中：年付（显示省多少）
- 价格文案
  - 月付：¥49 / 月
  - 年付：¥399 / 年（省 x%）
- CTA：开始 7 天试用 / 立即订阅
- 次要：恢复购买

#### 风险反转与条款（底部）
- 文案：随时取消。试用到期自动降级，不额外扣费（按 Google Play 规则）
- 文案：购买将通过 Google Play 完成

### 18.5 Profile（订阅自助中心）
- 卡片 1：当前状态
  - 标题：当前方案：Free / Pro
  - 字段：到期时间、自动续费状态
- 卡片 2：管理订阅
  - 按钮：管理订阅（跳系统订阅页）
  - 按钮：恢复购买（调用校验）
- 卡片 3：帮助
  - 入口：订阅没生效 / 如何取消 / 推送收不到 / 联系我们

### 18.6 订阅异常与恢复购买（必须有明确反馈）
- 触发入口：Paywall / Profile
- Loading 文案：正在同步订阅状态…
- 成功文案：已恢复 Pro 权益，有效期至 xxxx-xx-xx
- 失败文案（分类型）
  - 未找到购买记录：没有可恢复的订阅
  - 网络问题：网络异常，请稍后重试
  - 需要登录：请先登录以同步权益

## 19. 指标看板与告警阈值（上线后每天照着看）
### 19.1 每日必看（核心漏斗）
- Landing View
- Signup
- Activation（启用模板 或 signal_view）
- paywall_view
- trial_started
- subscribe_success
- Renewal（后续）

### 19.2 分发质量（稳定性护城河）
- Delivery success rate（目标 > 99%）
- Queue delay P50 / P90（目标：P90 < 30s）
- Dispatch delay P50 / P90（目标：P90 < 10s）
- Open rate（Delivered→Open，目标：> 25%）

### 19.3 告警阈值（触发就要排查）
- 过去 15 分钟 delivery 失败率 > 1%：立刻告警
- 队列深度持续增长且 oldest_due_seconds > 120：告警
- paywall_view 激增但 subscribe_success 不增长：告警（支付/恢复购买链路可能坏）
- signal_view 下降 > 30% 且 DAU 不变：告警（内容/入口/崩溃）

## 20. 上线检查清单（灰度前必做）
### 20.1 付费与权益
- Free/Trial/Pro 三种身份的锁定逻辑一致（列表与详情）
- 试用到期后自动降级，UI 与后台一致
- 管理订阅入口可用（打开系统订阅页）
- 恢复购买在弱网/无购买/已过期三种情况下反馈正确

### 20.2 推送与打扰控制
- 推送点击落地到正确 Signal Detail
- 新手默认不轰炸（默认模板数量受控）
- 安静时段/频控开关生效（即使暂不开发也要有策略与默认值）

### 20.3 增长与分享
- 分享页在 Twitter/Telegram 可读（标题/描述/卡片）
- Open App 深链可用（安装与未安装两种路径都不崩）
- CTA 文案统一（不要“去看看”，要“解锁/开启提醒”）

### 20.4 数据与隐私
- 隐私说明、免责声明在 App 内可找到
- 埋点不记录敏感信息（完整钱包地址可脱敏）

## 21. 实验计划（只做能提升收入/留存的）
### 21.1 Paywall 首屏实验
- A：先证据后价格
- B：先价格后证据
- 评价指标：trial_started、subscribe_success、退款/取消率（延后观察）

### 21.2 Onboarding 默认模板实验
- A：默认开 1 个模板
- B：默认开 2 个模板
- 评价指标：D1 留存、推送关闭率、卸载 proxy（如可获取）

### 21.3 文案实验（风险反转）
- A：突出“随时取消”
- B：突出“领先时间”
- 评价指标：paywall_view→trial_started 转化

## 22. FAQ（产品内自助内容提纲）
- 订阅没生效怎么办？（先点恢复购买）
- 如何取消订阅？（去系统订阅管理）
- 为什么我没收到推送？（权限/省电/安静时段/网络）
- 试用结束会扣费吗？（按 Google Play 规则说明）
- PolyPulse 是否提供投资建议？（免责声明）

## 附录 A：数据库表结构（DDL）
```sql
CREATE TABLE subscriptions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  platform TEXT NOT NULL,
  plan_id TEXT NOT NULL,
  status TEXT NOT NULL,
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  auto_renew INTEGER NOT NULL DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(user_id, platform, plan_id)
);

CREATE TABLE entitlements (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  tier TEXT NOT NULL,
  feature_key TEXT NOT NULL,
  is_enabled INTEGER NOT NULL DEFAULT 1,
  quota INTEGER,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(tier, feature_key)
);

CREATE TABLE user_entitlements (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  tier TEXT NOT NULL,
  effective_at DATETIME NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transactions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  platform TEXT NOT NULL,
  order_id TEXT NOT NULL,
  product_id TEXT NOT NULL,
  purchase_token TEXT NOT NULL,
  purchase_state TEXT NOT NULL,
  amount INTEGER NOT NULL,
  currency TEXT NOT NULL,
  purchased_at DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(platform, order_id)
);
```

## 附录 B：Feature Flags（DDL）
```sql
CREATE TABLE feature_flags (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  feature_key TEXT NOT NULL,
  enabled INTEGER NOT NULL,
  tier TEXT NOT NULL
);
```
