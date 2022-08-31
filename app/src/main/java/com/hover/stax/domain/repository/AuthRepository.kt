package com.hover.stax.domain.repository

import com.hover.stax.data.remote.dto.authorization.AuthResponse
import com.hover.stax.domain.model.TokenInfo

interface AuthRepository {

    suspend fun authorizeClient(idToken: String): AuthResponse

    suspend fun fetchTokenInfo(code: String): TokenInfo

    suspend fun refreshTokenInfo(): TokenInfo

    suspend fun getTokenInfo(): TokenInfo?

    suspend fun saveTokenInfo(tokenInfo: TokenInfo)

    suspend fun deleteTokenInfo()
}