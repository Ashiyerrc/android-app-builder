package com.ashiyerrc.appbuilder.ai

import com.ashiyerrc.appbuilder.ai.engine.PromptToSchemaEngine
import com.ashiyerrc.appbuilder.ai.fallback.ModelFallbackManager
import com.ashiyerrc.appbuilder.ai.providers.ClaudeFallbackProvider
import com.ashiyerrc.appbuilder.ai.providers.GeminiProvider
import com.ashiyerrc.appbuilder.ai.server.AiServer
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main() {
    logger.info("Initializing AI Server...")

    // Get API keys from environment
    val geminiApiKey = System.getenv("GEMINI_API_KEY")
        ?: throw IllegalArgumentException("GEMINI_API_KEY environment variable not set")
    
    val claudeApiKey = System.getenv("CLAUDE_API_KEY") ?: "optional"
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val host = System.getenv("HOST") ?: "0.0.0.0"

    logger.info("Gemini API Key: ${if (geminiApiKey.isNotBlank()) "✓ Configured" else "✗ Missing"}")
    logger.info("Claude API Key: ${if (claudeApiKey.isNotBlank() && claudeApiKey != "optional") "✓ Configured" else "✗ Not configured"}")

    // Create HTTP client for Claude
    val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(java.util.concurrent.TimeUnit.SECONDS, 30)
            }
        }
    }

    // Initialize providers
    val geminiProvider = GeminiProvider(geminiApiKey)
    val claudeProvider = ClaudeFallbackProvider(claudeApiKey, httpClient)

    val providers = listOf(
        geminiProvider,
        if (claudeProvider.isAvailable()) claudeProvider else null
    ).filterNotNull()

    logger.info("Loaded providers: ${providers.map { it.getProviderName() }}")

    // Create fallback manager
    val fallbackManager = ModelFallbackManager(providers)

    // Create prompt engine
    val promptEngine = PromptToSchemaEngine()

    // Create and start server
    val server = AiServer(
        port = port,
        host = host,
        fallbackManager = fallbackManager,
        promptEngine = promptEngine
    )

    server.start()
}
