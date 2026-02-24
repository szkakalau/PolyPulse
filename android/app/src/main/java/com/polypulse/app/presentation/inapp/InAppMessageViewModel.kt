package com.polypulse.app.presentation.inapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polypulse.app.data.remote.dto.InAppMessageDto
import com.polypulse.app.data.repository.InAppMessageRepository
import com.polypulse.app.data.repository.AnalyticsRepository
import com.polypulse.app.data.inapp.InAppMessageStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.polypulse.app.data.remote.dto.AnalyticsEventRequest

class InAppMessageViewModel(
    private val inAppMessageRepository: InAppMessageRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val inAppMessageStore: InAppMessageStore
) : ViewModel() {

    private val _inAppMessage = MutableStateFlow<InAppMessageDto?>(null)
    val inAppMessage: StateFlow<InAppMessageDto?> = _inAppMessage

    fun checkForMessage() {
        viewModelScope.launch {
            try {
                val lastShownAt = inAppMessageStore.getLastShownAt()
                if (lastShownAt != null) {
                    val now = System.currentTimeMillis()
                    if (now - lastShownAt < 12 * 60 * 60 * 1000) {
                        return@launch
                    }
                }
                val message = inAppMessageRepository.getInAppMessage()
                val lastMessageId = inAppMessageStore.getLastMessageId()
                if (message != null && message.id == lastMessageId) {
                    return@launch
                }
                _inAppMessage.value = message
                message?.let {
                    inAppMessageStore.updateLastShown(it.id, System.currentTimeMillis())
                    trackEvent(
                        "in_app_message_view",
                        mapOf(
                            "message_id" to it.id,
                            "message_type" to it.type
                        )
                    )
                }
            } catch (e: Exception) {
                // Handle error, maybe log it
            }
        }
    }

    fun onCtaClicked(message: InAppMessageDto, userTier: String?) {
        val properties = mutableMapOf(
            "message_id" to message.id,
            "message_type" to message.type,
            "action" to message.ctaAction
        )
        if (userTier != null) {
            properties["user_tier"] = userTier
        }
        trackEvent(
            "in_app_message_cta_click",
            properties
        )
        onMessageDismissed()
    }

    fun onMessageDismissed() {
        _inAppMessage.value?.let {
            trackEvent(
                "in_app_message_dismiss",
                mapOf(
                    "message_id" to it.id,
                    "message_type" to it.type
                )
            )
        }
        _inAppMessage.value = null
    }

    private fun trackEvent(name: String, properties: Map<String, String>) {
        viewModelScope.launch {
            try {
                analyticsRepository.trackEvent(AnalyticsEventRequest(name, properties))
            } catch (e: Exception) {
                // Log error
            }
        }
    }
}
