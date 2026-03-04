from pydantic import BaseModel
from typing import Optional, List

class UserRegister(BaseModel):
    email: str
    password: str

class UserLogin(BaseModel):
    email: str
    password: str

class Token(BaseModel):
    access_token: str
    token_type: str

class UserResponse(BaseModel):
    id: int
    email: str
    created_at: str

class BillingVerifyRequest(BaseModel):
    purchaseToken: str
    productId: str
    platform: str

class SubscriptionInfo(BaseModel):
    status: str
    planId: str
    startAt: str
    endAt: str
    autoRenew: bool

class EntitlementFeature(BaseModel):
    key: str
    enabled: bool
    quota: Optional[int] = None

class EntitlementsResponse(BaseModel):
    tier: str
    features: List[EntitlementFeature]
    effectiveAt: Optional[str] = None
    expiresAt: Optional[str] = None

class BillingVerifyResponse(BaseModel):
    status: str
    subscription: SubscriptionInfo
    entitlements: EntitlementsResponse

class BillingStatusResponse(BaseModel):
    status: str
    planId: Optional[str] = None
    startAt: Optional[str] = None
    endAt: Optional[str] = None
    autoRenew: Optional[bool] = None

class BillingWebhookRequest(BaseModel):
    eventType: str
    purchaseToken: Optional[str] = None
    orderId: Optional[str] = None
    status: Optional[str] = None
    startAt: Optional[str] = None
    endAt: Optional[str] = None

class SignalEvidence(BaseModel):
    sourceType: str
    triggeredAt: str
    marketId: str
    makerAddress: str
    evidenceUrl: str
    dedupeKey: str

class SignalResponse(BaseModel):
    id: int
    title: str
    content: Optional[str] = None
    locked: bool
    tierRequired: str
    createdAt: str
    evidence: Optional[SignalEvidence] = None

class PaywallPlan(BaseModel):
    id: str
    name: str
    price: int
    currency: str
    period: str
    trialDays: int

class PaywallResponse(BaseModel):
    plans: List[PaywallPlan]

class InAppMessageResponse(BaseModel):
    id: str
    type: str
    title: str
    body: str
    ctaText: str
    ctaAction: str
    plans: List[PaywallPlan]

class TrialStartResponse(BaseModel):
    status: str
    tier: str
    expiresAt: str

class NotificationRegisterRequest(BaseModel):
    token: str

class NotificationSendRequest(BaseModel):
    userId: int
    signalId: int

class NotificationSettingsResponse(BaseModel):
    enabled: bool

class NotificationSettingsUpdateRequest(BaseModel):
    enabled: bool

class AnalyticsEventRequest(BaseModel):
    eventName: str
    properties: Optional[dict] = None

class DailyPulseResponse(BaseModel):
    id: int
    title: str
    summary: str
    content: str
    createdAt: str

class SignalStatsResponse(BaseModel):
    signals7d: int
    evidence7d: int

class SignalCredibilityHistogramItem(BaseModel):
    bucket: str
    count: int

class SignalCredibilityWindowResponse(BaseModel):
    windowDays: int
    signalsTotal: int
    signalsWithEvidence: int
    evidenceRate: float
    evaluatedTotal: int
    hitTotal: int
    hitRate: float
    hitRateCiLow: float
    hitRateCiHigh: float
    latencyCount: int
    latencyP50Seconds: Optional[int] = None
    latencyP90Seconds: Optional[int] = None
    latencyHistogram: List[SignalCredibilityHistogramItem]
    leadCount: int
    leadP50Seconds: Optional[int] = None
    leadP90Seconds: Optional[int] = None
    leadHistogram: List[SignalCredibilityHistogramItem]

class SignalCredibilityResponse(BaseModel):
    window7d: SignalCredibilityWindowResponse
    window30d: SignalCredibilityWindowResponse

class AdminSignalEvaluationRequest(BaseModel):
    isHit: bool
    leadSeconds: Optional[int] = None
    evaluatedAt: Optional[str] = None

class DeliveryWindowResponse(BaseModel):
    windowDays: int
    attemptsTotal: int
    queued: int
    delayed: int
    sent: int
    failed: int
    noTokens: int
    disabled: int
    successRate: float
    pushOpenCount: int
    clickThroughRate: float
    queueDelayP50Seconds: Optional[int] = None
    queueDelayP90Seconds: Optional[int] = None
    dispatchDelayP50Seconds: Optional[int] = None
    dispatchDelayP90Seconds: Optional[int] = None

class DeliveryObservabilityResponse(BaseModel):
    window1d: DeliveryWindowResponse
    window7d: DeliveryWindowResponse
    redisQueueDepth: Optional[int] = None
    redisOldestDueSeconds: Optional[int] = None

class ReferralCodeResponse(BaseModel):
    code: str

class ReferralRedeemRequest(BaseModel):
    code: str

class ReferralRedeemResponse(BaseModel):
    status: str

class FeatureFlagResponse(BaseModel):
    key: str
    enabled: bool

class MetricsResponse(BaseModel):
    users: int
    subscriptions: int
    signals: int
    alerts: int
    daily_pulse: int

class MonitorAlertRequest(BaseModel):
    level: str
    message: str
    source: Optional[str] = None

class AdminSignalCreateRequest(BaseModel):
    title: str
    content: str
    tierRequired: str = "free"
    broadcast: bool = False
