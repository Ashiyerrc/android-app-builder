package com.ashiyerrc.appbuilder.ai.providers

import com.ashiyerrc.appbuilder.ai.domain.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class ClaudeFallbackProvider(
    private val apiKey: String,
    private val httpClient: HttpClient
) : AiProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generateResponse(request: AiRequest): AiResponse {
        return try {
            val response = httpClient.post("https://api.anthropic.com/v1/messages") {
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "model": "claude-3-haiku-20240307",
                        "max_tokens": ${request.maxTokens},
                        "temperature": ${request.temperature},
                        "messages": [{"role": "user", "content": "${request.prompt}"}]
                    }
                """.trimIndent())
            }

            val text = response.bodyAsText()
            val jsonResponse = Json.parseToJsonElement(text).jsonObject
            val content = jsonResponse["content"]?.jsonObject?.get("0")?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""

            AiResponse(
                id = System.currentTimeMillis().toString(),
                response = content,
                model = "claude-3-haiku",
                usage = TokenUsage(
                    inputTokens = estimateTokens(request.prompt),
                    outputTokens = estimateTokens(content),
                    totalTokens = estimateTokens(request.prompt) + estimateTokens(content)
                )
            )
        } catch (e: Exception) {
            logger.error("Error generating response from Claude", e)
            throw e
        }
    }

    override suspend fun generateAppSchema(request: AppBuilderAiRequest): AppBuilderAiResponse {
        val prompt = """Generate app schema from: ${request.prompt}"""
        return AppBuilderAiResponse(
            schema = mapOf(
                "appName" to request.appContext.appName,
                "packageName" to request.appContext.packageName
            ),
            screens = emptyList(),
            confidence = 0.6,
            suggestions = listOf("Use Gemini provider for better schema generation"),
            model = "claude-3-haiku"
        )
    }

    override suspend fun generateCode(request: AppBuilderAiRequest): GeneratedCodeResponse {
        val response = generateResponse(
            AiRequest(
                prompt = "Generate Kotlin Compose code: ${request.prompt}",
                model = "claude-3-haiku",
                maxTokens = 4096
            )
        )

        return GeneratedCodeResponse(
            code = response.response,
            language = "kotlin",
            type = "screen",
            fileStructure = mapOf("screen" to response.response)
        )
    }

    override suspend fun analyzeAndOptimize(request: AppBuilderAiRequest): OptimizationResponse {
        val response = generateResponse(
            AiRequest(
                prompt = "Optimize this code: ${request.prompt}",
                model = "claude-3-haiku",
                maxTokens = 2048
            )
        )

        return OptimizationResponse(
            optimizations = listOf(response.response),
            performanceTips = emptyList(),
            securitySuggestions = emptyList()
        )
    }

    override fun getProviderName(): String = "Claude 3 Haiku (Fallback)"

    override fun isAvailable(): Boolean = apiKey.isNotBlank() && apiKey != "optional"

    private fun estimateTokens(text: String): Int {
        return (text.split("\\s+".toRegex()).size * 1.3).toInt()
    }
}
