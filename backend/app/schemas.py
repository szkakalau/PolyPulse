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
