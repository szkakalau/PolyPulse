# PolyPulse API 文档

## 📋 概述
PolyPulse 是一个专业的加密货币信号和鲸鱼活动追踪平台API。本文档提供了所有可用端点的详细说明。

## 🔐 认证方式
所有受保护的API端点都需要Bearer Token认证。

### 获取访问令牌
```bash
POST /token
Content-Type: application/x-www-form-urlencoded

username=user@example.com&password=yourpassword
```

响应示例:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

### 使用访问令牌
```http
GET /protected-endpoint
Authorization: Bearer <access_token>
```

## 📊 API 端点

### 健康检查
```http
GET /health
```
检查API服务状态。

**响应:**
```json
{
  "status": "ok",
  "timestamp": "2024-03-04T10:30:00.000000"
}
```

### 用户注册
```http
POST /register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "name": "John Doe"
}
```

### 获取当前用户信息
```http
GET /users/me
Authorization: Bearer <token>
```

### 信号统计
```http
GET /signals/stats
Authorization: Bearer <token>
```

**响应:**
```json
{
  "signals7d": 150,
  "evidence7d": 120
}
```

### 鲸鱼交易数据
```http
GET /api/whales
Authorization: Bearer <token>
```

**响应:**
```json
[
  {
    "trade_id": "12345-abcde",
    "market_question": "Bitcoin Price Prediction",
    "outcome": "UP",
    "side": "BUY",
    "price": 0.85,
    "size": 1000.0,
    "value_usd": 850.0,
    "timestamp": "2024-03-04T10:30:00",
    "maker_address": "0x1234..."
  }
]
```

### 仪表板统计
```http
GET /dashboard/stats
Authorization: Bearer <token>
```

### 信号可信度分析
```http
GET /insights/credibility
Authorization: Bearer <token>
```

## 📈 监控端点

### Prometheus指标
```http
GET /metrics
```
提供Prometheus格式的性能指标。

## 🔧 错误处理

### 错误响应格式
```json
{
  "detail": "Error message description"
}
```

### 常见错误码
- `400 Bad Request` - 请求参数错误
- `401 Unauthorized` - 认证失败
- `403 Forbidden` - 权限不足
- `404 Not Found` - 资源不存在
- `500 Internal Server Error` - 服务器内部错误

## 🚀 性能特性

### 响应时间基准
| 端点 | 平均响应时间 | 推荐QPS |
|------|-------------|---------|
| /health | < 20ms | 50 |
| /signals/stats | < 50ms | 30 |
| /api/whales | < 150ms | 20 |
| /dashboard/stats | < 200ms | 15 |

### 速率限制
当前版本未实施严格的速率限制，但建议：
- 单个IP: 最大60请求/分钟
- 单个用户: 最大120请求/分钟

## 📋 数据格式

### 时间格式
所有时间戳使用ISO 8601格式: `YYYY-MM-DDTHH:MM:SS.ssssss`

### 数字格式
- 价格: 浮点数，精确到4位小数
- 数量: 浮点数，精确到2位小数
- 金额: 浮点数，精确到2位小数

## 🔗 相关资源

- [OpenAPI Schema](http://localhost:8000/openapi.json)
- [Prometheus Metrics](http://localhost:8000/metrics)
- [健康检查](http://localhost:8000/health)

## 📞 支持
如有问题，请联系开发团队或查看服务器日志获取详细错误信息。