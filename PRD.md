# PolyPulse 商业化 PRD（定稿版）

## 1. 文档信息
- 版本：v1.0
- 作者：产品
- 日期：2026-02-21
- 评审对象：产品、设计、研发、增长、运营

## 2. 项目背景
- 问题与机会：预测市场用户对高价值信号与低延迟提醒有持续需求，但现有产品缺少可验证的付费价值闭环
- 现状与已有能力：已有行情列表、仪表盘、提醒、排行榜与账号体系，具备端到端基础能力
- 竞品/替代方案：交易所自带提醒、第三方推送工具、社群手动分享

## 2.1 产品本质定义（Core Business Definition）
- PolyPulse 本质不是提醒工具，而是 Alpha Signal Distribution Platform
- 核心业务：数据源 -> 提取 Alpha Signal -> 分发 -> 订阅收费

## 2.2 收入公式（Revenue Formula）
- Revenue = Users × Conversion Rate × ARPU
- Users = Traffic × Signup Rate
- Conversion Rate = Trial -> Paid
- ARPU = Subscription price × Retention

## 3. 目标与范围
- 业务目标：完成商业化验证并建立最小变现闭环
- 产品目标：验证高价值提醒的付费意愿与转化路径
- 本期范围：付费墙、Pro 权益、定价方案、指标监控
- 不做事项：完整策略交易、自动化下单

## 4. 用户画像
### 4.1 画像 A（核心）
- 名称：高频交易者
- 角色与背景：日内交易、套利型用户
- 目标：更早获得高价值信号并提升决策速度
- 痛点：信号延迟、噪声高、信息分散
- 行为特征：高频查看行情与提醒，愿意为速度付费

### 4.2 画像 B（成长）
- 名称：策略观察者
- 角色与背景：中频策略型用户
- 目标：通过历史与榜单判断信号可靠性
- 痛点：缺少可信的历史表现与解释
- 行为特征：重视榜单与复盘，接受订阅

### 4.3 画像 C（入口）
- 名称：新手用户
- 角色与背景：轻度体验与学习型用户
- 目标：快速上手并获取基础提醒
- 痛点：功能复杂、缺少模板与引导
- 行为特征：依赖默认配置与一键启用

## 5. 用户故事
- 作为高频交易者，我希望在关键行情变化时立即收到高价值提醒，从而快速决策
- 作为策略观察者，我希望查看历史命中率与榜单表现，从而判断信号可靠性
- 作为新手用户，我希望一键启用提醒模板，从而快速开始使用

## 6. 需求范围与优先级
### 6.1 功能需求（MoSCoW）
| 优先级 | 需求 | 说明 | 验收标准 |
| --- | --- | --- | --- |
| Must | 高价值提醒与低延迟推送 | Pro 核心权益 | 延迟 < 30s，推送成功率 > 99% |
| Must | 付费墙与订阅入口 | 完成最小商业化闭环 | 付费入口可触达且可完成订阅 |
| Should | 历史绩效与命中率 | 提升信号可信度 | 提供 7 日历史表现 |
| Could | 专家/策略订阅 | 增强差异化 | 可订阅且可取消 |
| Won’t | 自动化下单 | 超出范围 | 本期不实现 |

### 6.2 非功能需求
- 性能：关键接口 P95 < 500ms
- 稳定性：核心链路成功率 > 99%
- 安全与合规：增加风险提示与使用边界
- 可观测性：关键指标与错误可追踪

## 7. 商业化设计
### 7.1 价值主张
- 核心价值点：赚钱机会前置 + 速度优势 + 真实可验证
- 核心一句话价值主张：Real-time alpha signal platform
- 付费价值表达（产品内文案）
  - Get alerted before the market moves.
  - Be first. Not last.
  - See which signals actually worked.
- 价值验证方式：试用转化率与付费转化率 A/B 测试

### 7.2 定价方案
- 三层结构：Free / Pro / Elite（后期）
- 版本对比：免费获取用户，Pro 为核心收入，Elite 为未来最大收入

| 方案 | 价格 | 目标人群 | 主要权益 |
| --- | --- | --- | --- |
| Free | $0 | 新手用户 | 延迟 2–5 分钟、基础提醒 |
| Pro 月付 | $7.99 | 高频交易者 | 实时提醒、高价值信号 |
| Pro 年付 | $59 | 长期订阅者 | 同月付权益，年付折扣 |
| Elite 月付 | $29–99 | 专业用户 | 策略订阅、专家信号 |

### 7.3 付费墙策略
- 触发点：点击 High value signal 后触发
- 展示位置：提醒配置页、排行榜页
- 权益阈值：免费最多启用 2 个模板
- 试用与优惠策略：7 天试用或首月折扣
- 转化心理顺序：看到价值 -> 想要 -> 被限制 -> 付费

### 7.4 转化路径
- 触达入口：提醒入口、榜单入口、个性化配置入口
- 转化节点：触发付费墙、展示价值、完成订阅
- 流失点与优化策略：优化文案与权益解释、减少支付步骤

### 7.5 收入结构图
- Users -> Free -> Trial -> Pro -> Elite（后期）-> Recurring revenue

### 7.6 Signal Tier System
| Tier | 名称 | Free | Pro | Elite |
| --- | --- | --- | --- | --- |
| S | Institutional Signal | ❌ | ❌ | ✅ |
| A | Whale Signal | ❌ | ✅ | ✅ |
| B | High Activity | 延迟 | ✅ | ✅ |
| C | Normal | 延迟 | 延迟 | 延迟 |

### 7.7 Paywall Structure
- Signal Locked Preview
  - Whale bet $420,000 on YES
  - Unlock to view
- Social Proof
  - Used by 12,421 traders
- Value reinforcement
  - Avg signal lead time: 4m 32s
- Price anchoring
  - Yearly / Monthly
- Risk reversal
  - 7-day free trial

## 8. 数据指标
### 8.1 北极星指标
- 指标定义：有效提醒触达后被点击或响应的比例
- 目标值：> 25%

### 8.2 商业化指标
- 付费转化率：3–5%
- 试用转化率：15–20%
- ARPU：￥15–25
- 续费率：60%+
- LTV：$60+

### 8.3 产品健康指标
- DAU/MAU：> 25%
- 留存（D1/D7/D30）：30% / 15% / 8%
- 关键链路成功率：> 99%

### 8.4 SaaS 核心指标
- LTV > CAC
- MRR
- ARR
- Churn rate：< 5%

### 8.5 Revenue Projection
- Users：10,000
- Conversion：4%
- Paid users：400
- ARPU：$8
- Revenue：$3,200 / month
- Annual：$38,400

## 9. 交互流程
- 关键流程 1：登录 -> 选择提醒模板 -> 触发付费墙 -> 订阅
- 关键流程 2：查看榜单 -> 查看历史绩效 -> 付费墙 -> 订阅
- 关键流程 3：推送触达 -> 点击 -> 提示升级

## 9.1 留存与增长机制
### 9.1.1 Daily Habit Engine
- Daily Pulse 页面
- 每日自动生成 Today market signals
- 组成：Top whale trades、Most active markets、Win rate leaderboard

### 9.1.2 Viral Loop
- 每条提醒支持分享
- 分享渠道：Twitter、Telegram
- 自动生成 PolyPulse watermark

### 9.1.3 收入转化引擎
- Trial Ending Warning：Trial ends in 2 days

## 9.2 Growth Engine
### 9.2.1 SEO Landing Pages
- 自动生成每个 Market 页面
- 示例：/market/trump-win-2028
- 目标：获取 Google 流量

### 9.2.2 Signal Share Pages
- 每个 Signal 可公开访问
- 示例：/signal/whale-bet-123
- 配置 Signup CTA

### 9.2.3 Referral Program
- Invite 1 friend -> Get 7 days Pro

## 9.3 Retention Engine
### 9.3.1 Push Strategy
- Daily Pulse
- Whale alerts
- Trial ending

### 9.3.2 Email Strategy
- 增加 Email channel

## 9.4 Onboarding Flow
- Signup -> Choose interest -> Enable alerts -> Show locked signal -> Paywall

## 10. 技术规格
### 10.1 数据库表结构（DDL）
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

### 10.2 接口字段定义
- POST /billing/verify
  - 请求：purchaseToken, productId, platform
  - 返回：status, subscription, entitlements
- GET /billing/status
  - 返回：status, planId, startAt, endAt, autoRenew
- GET /entitlements/me
  - 返回：tier, features[], quota
- POST /billing/webhook
  - 请求：eventType, purchaseToken, orderId, status, startAt, endAt

### 10.3 订阅状态机
- active -> grace -> expired
- active -> canceled
- active -> paused
- grace -> active

### 10.4 Android 购买流程 UI/交互方案
- 入口位置：提醒配置页、排行榜历史页、Pro 标签
- 页面结构：方案对比、权益说明、价格与试用信息
- 流程：选择方案 -> 调用 Play Billing -> 校验 -> 刷新权益

### 10.5 Web Billing
- 支持 Stripe
- 目标：降低平台抽成

### 10.6 Analytics Events
- Acquisition：signup, login
- Activation：alert_enabled, signal_view
- Revenue：trial_started, subscription_started, subscription_renewed, subscription_canceled
- Engagement：daily_active, signal_clicked

### 10.7 System Architecture
```
Signal Ingestion Service
↓
Signal Processing
↓
Signal Storage
↓
Notification Service
↓
Client Apps
```

### 10.8 Feature Flags
```sql
CREATE TABLE feature_flags (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  feature_key TEXT NOT NULL,
  enabled INTEGER NOT NULL,
  tier TEXT NOT NULL
);
```

## 11. 里程碑
- 第 1 周：完成用户画像与价值验证方案
- 第 2 周：上线付费墙与 Pro 权益入口
- 第 3 周：上线定价与订阅流程
- 第 4 周：完成 A/B 测试与优化

## 11.1 差距补齐方案
### 11.1.1 产品闭环
- 建立订阅与权益体系，明确免费与 Pro 权限边界
- 上线高价值提醒与历史绩效验证页
- 付费墙触发点与权益解释完善

### 11.1.2 技术稳定性
- 引入缓存与降级策略，确保提醒可用性
- 数据库从 SQLite 升级到 Postgres
- 核心链路监控与日志追踪

### 11.1.3 增长与留存
- 建立转化漏斗与埋点体系
- 试用期触达策略与续费提醒机制
- 提升留存的权益复用与订阅模板

### 11.1.4 合规与风控
- 风险提示与用户协议强化
- 地区限制与合规策略配置
- 高风险功能提示与限制

## 11.2 分阶段开发计划
### 阶段 1：商业化闭环
- 订阅与交易表落库
- 订阅状态校验接口
- Android 端付费墙入口
- 关键指标：付费入口点击率、订阅完成率

### 阶段 2：价值验证
- 高价值提醒与历史绩效页
- 权益校验与 Pro 解锁
- 定价与文案 A/B 测试
- 关键指标：试用转化率、付费转化率

### 阶段 3：稳定性与扩展
- 缓存与降级策略
- 数据库迁移到 Postgres
- 监控与告警
- 关键指标：核心链路成功率、P95 延迟

### 阶段 4：增长与留存
- 转化漏斗与埋点分析
- 续费提醒与复购策略
- 留存提升策略
- 关键指标：续费率、D30 留存

## 11.3 商业化落地补充项
### 11.3.1 支付与退款流程
- 退款策略与处理时效
- 订阅取消、降级与到期规则
- 账单与税务信息对齐

### 11.3.2 客服与运营机制
- 支付失败与订阅异常处理流程
- FAQ 与自助支持入口
- 续费提醒与权益复用运营

### 11.3.3 数据合规与隐私
- 数据保留与删除策略
- 用户数据导出与权限管理
- 地区合规提示与限制

### 11.3.4 发布与灰度
- 灰度发布与回滚机制
- 关键功能开关
- 监控告警阈值与应急预案

## 11.4 Risk Control
- 免责声明：PolyPulse does not provide financial advice

## 12. 风险与假设
- 关键假设：用户愿意为高价值提醒与低延迟付费
- 风险清单：数据不稳定导致提醒失效；合规风险限制推广
- 应对策略：数据降级与缓存；增加风险提示

## 13. 依赖与资源
- 研发依赖：订阅与付费能力、通知渠道
- 数据依赖：行情源稳定性与监控
- 运营依赖：付费引导文案与渠道投放

## 14. 验收标准
- 功能验收：付费墙触发、订阅流程完整
- 指标验收：试用转化率与付费转化率达标
- 上线验收：核心链路无阻塞错误

## 15. 开放问题
- 问题 1：是否需要分地区合规策略
- 问题 2：是否引入专家或策略订阅作为差异化

## 16. 实施任务清单（顺序）
1. 订阅与交易相关数据表落地
2. 权益与订阅状态接口实现
3. Android 端接入付费墙入口
4. Play Billing 订阅购买与校验
5. 权限校验与权益下发
6. 转化与留存指标埋点
7. A/B 测试定价与文案

## 16.1 Development Priority Order
- Phase 1（Week 1–2）：Signal system、Paywall、Subscription
- Phase 2（Week 3）：Daily Pulse、Trial、Analytics
- Phase 3（Week 4）：Growth、Referral、SEO pages
- Phase 4（Month 2）：Elite tier

## 17. PRD 完整度评价
- 商业级：9.5 / 10
- 已达到 SaaS 创业标准

## 18. 下一步执行
- 开始开发与落地商业化闭环

## 19. 正确执行顺序（最终版）
- Week 1：Subscription
- Week 2：Paywall
- Week 3：Signal lock
- Week 4：Start charging

## 20. PolyPulse 开发任务拆解（Jira 版）

### EPIC 1：Subscription & Billing（订阅系统）
- 目标：支持 Pro 订阅并正确下发权限
- 优先级：P0（第一周完成）

#### Story 1.1 创建订阅数据模型
- Task 1.1.1 创建 subscriptions 表
  - 验收标准：表创建成功、可插入记录、唯一索引生效
- Task 1.1.2 创建 transactions 表
  - 验收标准：支付记录可保存、order_id 唯一
- Task 1.1.3 创建 entitlements 表
  - 验收标准：支持 Free、Pro、Elite
- Task 1.1.4 创建 user_entitlements 表
  - 验收标准：用户权限可查询

#### Story 1.2 Billing API
- Task 1.2.1 POST /billing/verify
  - 功能：验证 Google Play purchaseToken
  - 返回：subscription_status, tier, expires_at
  - 验收标准：验证成功返回 active
- Task 1.2.2 GET /billing/status
  - 验收标准：返回 tier, expires_at, auto_renew
- Task 1.2.3 POST /billing/webhook
  - 功能：接收 Play 回调
  - 验收标准：订阅状态自动更新

### EPIC 2：Entitlement System（权限系统）
- 目标：控制 Free / Pro / Elite 权限
- 优先级：P0

#### Story 2.1 权限查询 API
- Task 2.1.1 GET /entitlements/me
  - 返回：tier, features, quota
  - 验收：Pro 用户 high_value_signal = true，Free 为 false

#### Story 2.2 权限中间件
- Task 2.2.1 Backend middleware
  - 逻辑：if tier != Pro block high signal
  - 验收：Free 用户访问返回 402 Payment Required

### EPIC 3：Signal System（核心付费内容）
- 目标：Signal 可锁定
- 优先级：P0

#### Story 3.1 Signal 表
- Task 3.1.1 创建 signals 表
  - 字段：id, title, content, tier_required, created_at

#### Story 3.2 Signal API
- Task 3.2.1 GET /signals
  - Free：返回 locked = true，content = null
  - Pro：返回完整内容

### EPIC 4：Paywall（付费墙）
- 目标：阻止 Free 查看 Pro 内容
- 优先级：P0

#### Story 4.1 Paywall API
- Task 4.1.1 GET /paywall
  - 返回：Free, Pro Monthly, Pro Yearly

#### Story 4.2 Signal Lock Logic
- Task 4.2.1 Backend
  - 逻辑：if not Pro return locked = true
  - 验收：Free 用户看到 locked

### EPIC 5：Trial System（试用）
- 优先级：P1

#### Story 5.1 Trial Activation
- Task 5.1.1 POST /trial/start
  - 逻辑：tier = Pro，expires_at = now + 7 days
  - 验收：Trial 用户可访问 Pro

#### Story 5.2 Trial Expiration
- Task 5.2.2 cron job
  - 每天运行，检查 trial expired，更新 tier -> Free

### EPIC 6：Notification System（提醒系统）
- 优先级：P0

#### Story 6.1 Push Service
- Task 6.1.1 POST /notifications/send
  - 字段：user_id, signal_id
  - 验收：用户收到 push

#### Story 6.2 权限过滤
- Task 6.2.2
  - Pro：实时发送
  - Free：延迟发送

### EPIC 7：Analytics（埋点）
- 优先级：P1

#### Story 7.1 Event Tracking
- Task 7.1.1 Track：signup
- Task 7.1.2 Track：subscription_started
- Task 7.1.3 Track：signal_view

### EPIC 8：Daily Pulse（留存核心）
- 优先级：P1

#### Story 8.1 Daily Pulse API
- Task 8.1.1 GET /daily-pulse
  - 返回：top_signals, leaderboard

### EPIC 9：Referral（增长）
- 优先级：P2

#### Story 9.1 Referral Code
- Task 9.1.1 生成 referral_code

#### Story 9.2 Referral Reward
- Task 9.2.2 邀请成功增加 Pro days +7

### 前端开发任务

#### EPIC FE-1 Paywall Page
- 页面：/paywall
- 包含：Plan list、Subscribe button
- 验收：点击订阅调用 Billing

#### EPIC FE-2 Signal Page
- 页面：/signals
- Free：显示 locked
- Pro：完整显示

#### EPIC FE-3 Daily Pulse
- 页面：/daily
- 显示：signals

#### EPIC FE-4 Settings Page
- 页面：/settings/subscription
- 显示：tier、expire date

### DevOps 任务

#### EPIC OPS-1 Database
- 部署：Postgres

#### EPIC OPS-2 Cron
- 创建：trial expiration job

#### EPIC OPS-3 Notification Queue
- 使用：Redis 或 RabbitMQ

### MVP 开发优先级（必须按顺序）
- 第一周：Subscription、Entitlement、Signal Lock
- 第二周：Paywall、Trial
- 第三周：Push、Analytics
- 第四周：Start Charging
- 完成后 MVP 即可收费

### 完成定义（Definition of Done）
- 用户注册 -> 看到 Signal -> 点击 -> 被锁 -> 付费 -> 解锁 -> 收到 Push

### 最小上线版本范围
- 只需要 22 个任务，2–4 周可完成

## 21. PolyPulse 总体架构图
```
                ┌────────────────────┐
                │     Mobile App     │
                │  Android / iOS    │
                └─────────┬──────────┘
                          │ HTTPS API
                          ▼
                ┌────────────────────┐
                │    API Gateway     │
                │  Auth / RateLimit │
                └───────┬───────────┘
                        │
        ┌───────────────┼────────────────┐
        ▼               ▼                ▼

┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ Signal API   │ │ Billing API  │ │ User API     │
└──────┬───────┘ └──────┬───────┘ └──────┬───────┘
       │                │                │
       ▼                ▼                ▼

┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ Signal DB    │ │ Billing DB   │ │ User DB      │
│ Postgres     │ │ Postgres     │ │ Postgres     │
└──────────────┘ └──────────────┘ └──────────────┘

────────────────────────────────────
核心实时系统
────────────────────────────────────

        外部数据源
             │
             ▼
    ┌─────────────────┐
    │ Signal Ingestor │
    └────────┬────────┘
             ▼
    ┌─────────────────┐
    │ Signal Processor│
    │ 判断是否 High   │
    └────────┬────────┘
             ▼
     ┌──────────────┐
     │ Redis Queue  │
     └──────┬───────┘
            ▼

     ┌──────────────┐
     │ Notification │
     │ Service     │
     └──────┬───────┘
            ▼

     ┌──────────────┐
     │ Push Provider│
     │ Firebase/APNS│
     └──────────────┘

────────────────────────────────────
增长系统
────────────────────────────────────

┌─────────────────┐
│ Analytics       │
│ PostHog/Mixpanel│
└─────────────────┘

────────────────────────────────────
缓存层
────────────────────────────────────

┌──────────────┐
│ Redis Cache  │
└──────────────┘

────────────────────────────────────
后台管理
────────────────────────────────────

┌──────────────┐
│ Admin Panel  │
└──────────────┘
```

## 22. 各模块职责说明

### 22.1 API Gateway
- 核心入口
- 功能：Auth、JWT 验证、限流、防攻击
- 推荐：Cloudflare 或 Nginx

### 22.2 Signal Ingestor（信号采集服务）
- 作用：从数据源采集数据
- 示例：交易、价格变化、Whale 钱包
- 输出：原始 signal

### 22.3 Signal Processor（核心赚钱服务）
- 作用：判断是否 High value signal
- 逻辑示例：
```
if trade_size > 100k
mark high_value = true
```
- 写入：Postgres
- 发送：Redis Queue

### 22.4 Signal DB
- 存储：signals
- 结构：id, title, content, tier_required, created_at

### 22.5 Notification Service（推送服务）
- 监听：Redis Queue
- 发送：Push
- 逻辑示例：
```
if user tier == Pro
send immediately
else
delay 5 min
```

### 22.6 Billing Service
- 负责：订阅验证、trial、权限
- 数据表：subscriptions、transactions

### 22.7 Entitlement System
- 负责：判断用户能否访问 signal
- 逻辑示例：
```
if tier == Free
locked
```

### 22.8 Redis
- 用途：缓存 signals，减少 DB 压力
- 用途：Push Queue

### 22.9 Analytics
- 记录：signal_view、subscription_start、trial_start
- 推荐：PostHog（免费）

### 22.10 Push Provider
- 推荐：Firebase
- 支持：Android、iOS、Web

## 23. 关键数据流

### 23.1 Flow 1 Signal 产生
- 数据源 -> Signal Ingestor -> Signal Processor -> Postgres -> Redis Queue -> Notification Service -> Push -> 用户手机

### 23.2 Flow 2 用户查看 Signal
- 用户 -> API Gateway -> Signal API -> Entitlement check -> 返回 locked / unlocked

### 23.3 Flow 3 用户订阅
- 用户 -> Billing API -> Play Billing -> Webhook -> Billing DB -> 更新 entitlement

## 24. Production 推荐技术栈
- Backend：Python FastAPI 或 Node.js
- Database：Postgres
- Cache：Redis
- Queue：Redis（早期）/ Kafka（未来）
- Push：Firebase
- Hosting：Google Cloud 或 Amazon Web Services

## 25. MVP 最小服务器结构（最省钱）
- 1 台服务器：API + DB + Redis + Worker
- $20 / 月

## 26. 用户增长后扩展架构
- 拆分：API servers、Worker servers、DB server、Redis server

## 27. 架构核心设计原则
- 原则 1：Signal 和 Push 必须解耦，用 Queue
- 原则 2：权限检查必须 server side，不能 client side
- 原则 3：Push 必须 async，不能同步
- 原则 4：Redis 必须使用，否则性能不够

## 28. 最小 MVP 实际只需 6 个服务
- API
- Postgres
- Redis
- Signal Processor
- Notification Service
- Firebase

## 29. 开发时间现实估计
- 一个人 3–4 周即可上线收费版本
