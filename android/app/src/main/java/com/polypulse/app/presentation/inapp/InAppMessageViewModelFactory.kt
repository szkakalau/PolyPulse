package com.polypulse.app.presentation.inapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.polypulse.app.data.repository.InAppMessageRepository
import com.polypulse.app.data.repository.AnalyticsRepository
import com.polypulse.app.data.inapp.InAppMessageStore

class InAppMessageViewModelFactory(
    private val inAppMessageRepository: InAppMessageRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val inAppMessageStore: InAppMessageStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InAppMessageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InAppMessageViewModel(inAppMessageRepository, analyticsRepository, inAppMessageStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
