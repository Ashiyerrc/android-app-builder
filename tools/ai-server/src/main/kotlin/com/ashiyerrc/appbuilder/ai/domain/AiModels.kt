package com.ashiyerrc.appbuilder.ai.domain

import kotlinx.serialization.Serializable

@Serializable
data class AiRequest(
    val prompt: String,
    val model: String = "gemini",
    val temperature: Double = 0.7,
    val maxTokens: Int = 2048,
    val context: Map<String, String> = emptyMap()
)

@Serializable
data class AiResponse(
    val id: String,
    val response: String,
    val model: String,
    val usage: TokenUsage,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int
)

@Serializable
data class AppBuilderAiRequest(
    val type: String,
    val prompt: String,
    val appContext: AppContext = AppContext(),
    val settings: Map<String, String> = emptyMap()
)

@Serializable
data class AppContext(
    val appName: String = "",
    val packageName: String = "",
    val existingScreens: List<String> = emptyList(),
    val targetPlatform: String = "android",
    val designSystem: String = "material3"
)

@Serializable
data class AppBuilderAiResponse(
    val schema: Map<String, Any> = emptyMap(),
    val screens: List<ScreenGenerationResult> = emptyList(),
    val confidence: Double = 0.0,
    val suggestions: List<String> = emptyList(),
    val model: String = "gemini",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ScreenGenerationResult(
    val screenName: String,
    val screenType: String,
    val fields: List<Map<String, String>> = emptyList(),
    val layout: String,
    val code: String? = null
)

@Serializable
data class GeneratedCodeResponse(
    val code: String,
    val language: String = "kotlin",
    val type: String,
    val fileStructure: Map<String, String> = emptyMap()
)

@Serializable
data class OptimizationResponse(
    val optimizations: List<String> = emptyList(),
    val performanceTips: List<String> = emptyList(),
    val securitySuggestions: List<String> = emptyList(),
    val refactoredCode: String? = null
)

enum class AiModelType {
    GEMINI, CLAUDE, FALLBACK
}

data class ModelProvider(
    val type: AiModelType,
    val name: String,
    val isAvailable: Boolean = true,
    val costTier: String = "free"
)