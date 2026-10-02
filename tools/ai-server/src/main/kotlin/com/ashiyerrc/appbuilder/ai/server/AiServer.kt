package com.ashiyerrc.appbuilder.ai.server

import com.ashiyerrc.appbuilder.ai.domain.*
import com.ashiyerrc.appbuilder.ai.engine.PromptToSchemaEngine
import com.ashiyerrc.appbuilder.ai.fallback.ModelFallbackManager
import com.ashiyerrc.appbuilder.ai.providers.AiProvider
import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.request.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class AiServer(
    private val port: Int = 8080,
    private val host: String = "0.0.0.0",
    private val fallbackManager: ModelFallbackManager,
    private val promptEngine: PromptToSchemaEngine
) {

    fun start() {
        logger.info("Starting AI Server on $host:$port")
        logger.info("Available providers: ${fallbackManager.getAvailableProviders()}")

        embeddedServer(Netty, port = port, host = host) {
            install(CORS) {
                anyHost()
                allowMethod(HttpMethod.Options)
                allowMethod(HttpMethod.Post)
                allowMethod(HttpMethod.Get)
                allowHeader(HttpHeaders.ContentType)
                allowHeader("Authorization")
            }

            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                })
            }

            configureRouting()
        }.start(wait = true)
    }

    private fun Application.configureRouting() {
        routing {
            // Health check
            get("/health") {
                call.respond(
                    mapOf(
                        "status" to "ok",
                        "providers" to fallbackManager.getAvailableProviders(),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }

            // Generic AI prompt endpoint
            post("/api/v1/ai/prompt") {
                try {
                    val request = call.receive<AiRequest>()
                    val (response, provider) = fallbackManager.generateResponse(request)
                    call.respond(
                        mapOf(
                            "success" to true,
                            "data" to response,
                            "provider" to provider
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Error processing prompt", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf(
                            "success" to false,
                            "error" to (e.message ?: "Unknown error")
                        )
                    )
                }
            }

            // Schema generation from prompt
            post("/api/v1/app-builder/schema") {
                try {
                    val request = call.receive<AppBuilderAiRequest>()

                    // First parse with local engine
                    val parsed = promptEngine.parsePrompt(request.prompt)
                    val localSchema = promptEngine.generateAppSchema(parsed, request.appContext)

                    // Try to enhance with AI provider
                    val (aiResponse, provider) = try {
                        fallbackManager.generateAppSchema(request)
                    } catch (e: Exception) {
                        logger.warn("AI provider failed, using local schema", e)
                        Pair(localSchema, "local-prompt-engine")
                    }

                    call.respond(
                        mapOf(
                            "success" to true,
                            "data" to aiResponse,
                            "provider" to provider,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Error generating schema", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf(
                            "success" to false,
                            "error" to (e.message ?: "Unknown error")
                        )
                    )
                }
            }

            // Code generation
            post("/api/v1/app-builder/generate-code") {
                try {
                    val request = call.receive<AppBuilderAiRequest>()
                    val (codeResponse, provider) = fallbackManager.generateCode(request)

                    call.respond(
                        mapOf(
                            "success" to true,
                            "data" to codeResponse,
                            "provider" to provider,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Error generating code", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf(
                            "success" to false,
                            "error" to (e.message ?: "Unknown error")
                        )
                    )
                }
            }

            // Layout optimization
            post("/api/v1/app-builder/optimize") {
                try {
                    val request = call.receive<AppBuilderAiRequest>()
                    val (optimization, provider) = fallbackManager.analyzeAndOptimize(request)

                    call.respond(
                        mapOf(
                            "success" to true,
                            "data" to optimization,
                            "provider" to provider,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Error optimizing", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf(
                            "success" to false,
                            "error" to (e.message ?: "Unknown error")
                        )
                    )
                }
            }

            // Code fixing
            post("/api/v1/app-builder/fix-code") {
                try {
                    val request = call.receive<AppBuilderAiRequest>()
                    val (codeResponse, provider) = fallbackManager.generateCode(
                        request.copy(
                            prompt = "Fix this code and explain issues:\n${request.prompt}"
                        )
                    )

                    call.respond(
                        mapOf(
                            "success" to true,
                            "data" to codeResponse,
                            "provider" to provider,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Error fixing code", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf(
                            "success" to false,
                            "error" to (e.message ?: "Unknown error")
                        )
                    )
                }
            }
        }
    }
}
