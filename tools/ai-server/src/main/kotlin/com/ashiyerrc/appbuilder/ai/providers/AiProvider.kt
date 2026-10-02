package com.ashiyerrc.appbuilder.ai.providers

import com.ashiyerrc.appbuilder.ai.domain.*

interface AiProvider {
    suspend fun generateResponse(request: AiRequest): AiResponse
    suspend fun generateAppSchema(request: AppBuilderAiRequest): AppBuilderAiResponse
    suspend fun generateCode(request: AppBuilderAiRequest): GeneratedCodeResponse
    suspend fun analyzeAndOptimize(request: AppBuilderAiRequest): OptimizationResponse
    fun getProviderName(): String
    fun isAvailable(): Boolean
}

data class ProviderResult<T>(
    val success: Boolean,
    val data: T? = null,
    val error: Exception? = null,
    val providerUsed: String = ""
)
