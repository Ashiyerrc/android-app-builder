package com.ashiyerrc.appbuilder.ai.fallback

import com.ashiyerrc.appbuilder.ai.domain.*
import com.ashiyerrc.appbuilder.ai.providers.AiProvider
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class ModelFallbackManager(
    private val providers: List<AiProvider>
) {

    suspend fun <T> executeWithFallback(
        operationName: String,
        operation: suspend (provider: AiProvider) -> T
    ): Pair<T?, String> {
        val availableProviders = providers.filter { it.isAvailable() }
        
        if (availableProviders.isEmpty()) {
            logger.error("No AI providers available")
            throw IllegalStateException("No AI providers available")
        }

        for (provider in availableProviders) {
            return try {
                logger.info("Executing $operationName with ${provider.getProviderName()}")
                val result = operation(provider)
                Pair(result, provider.getProviderName())
            } catch (e: Exception) {
                logger.warn("${provider.getProviderName()} failed for $operationName: ${e.message}")
                if (provider == availableProviders.last()) {
                    throw e
                }
                continue
            }
        }

        throw IllegalStateException("All providers failed for $operationName")
    }

    suspend fun generateResponse(
        request: AiRequest,
        preferredProvider: String? = null
    ): Pair<AiResponse, String> {
        val (response, provider) = executeWithFallback("generateResponse") { p ->
            p.generateResponse(request)
        }
        return Pair(response!!, provider)
    }

    suspend fun generateAppSchema(
        request: AppBuilderAiRequest,
        preferredProvider: String? = null
    ): Pair<AppBuilderAiResponse, String> {
        val (response, provider) = executeWithFallback("generateAppSchema") { p ->
            p.generateAppSchema(request)
        }
        return Pair(response!!, provider)
    }

    suspend fun generateCode(
        request: AppBuilderAiRequest,
        preferredProvider: String? = null
    ): Pair<GeneratedCodeResponse, String> {
        val (response, provider) = executeWithFallback("generateCode") { p ->
            p.generateCode(request)
        }
        return Pair(response!!, provider)
    }

    suspend fun analyzeAndOptimize(
        request: AppBuilderAiRequest,
        preferredProvider: String? = null
    ): Pair<OptimizationResponse, String> {
        val (response, provider) = executeWithFallback("analyzeAndOptimize") { p ->
            p.analyzeAndOptimize(request)
        }
        return Pair(response!!, provider)
    }

    fun getAvailableProviders(): List<String> {
        return providers.filter { it.isAvailable() }.map { it.getProviderName() }
    }
}
