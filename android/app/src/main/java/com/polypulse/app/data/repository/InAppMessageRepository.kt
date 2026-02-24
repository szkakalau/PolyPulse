package com.polypulse.app.data.repository

import com.polypulse.app.data.remote.BackendApiProvider
import com.polypulse.app.data.remote.dto.InAppMessageDto
import com.polypulse.app.data.auth.TokenManager
import kotlinx.coroutines.flow.first

class InAppMessageRepository(
    private val backendApiProvider: BackendApiProvider,
    private val tokenManager: TokenManager
) {
    suspend fun getInAppMessage(): InAppMessageDto? {
        val token = tokenManager.token.first() ?: return null
        return backendApiProvider.call { it.getInAppMessage("Bearer $token") }
    }
}
