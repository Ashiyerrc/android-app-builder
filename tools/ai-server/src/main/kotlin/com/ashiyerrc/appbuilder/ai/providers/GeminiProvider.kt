package com.ashiyerrc.appbuilder.ai.providers

import com.ashiyerrc.appbuilder.ai.domain.*
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class GeminiProvider(
    private val apiKey: String
) : AiProvider {

    private val model = GenerativeModel(
        modelName = "gemini-1.5-pro",
        apiKey = apiKey
    )

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generateResponse(request: AiRequest): AiResponse {
        return try {
            val prompt = buildPrompt(request)
            val response = model.generateContent(
                content {
                    text(prompt)
                }
            )

            val text = response.text ?: "No response generated"

            AiResponse(
                id = System.currentTimeMillis().toString(),
                response = text,
                model = "gemini-1.5-pro",
                usage = TokenUsage(
                    inputTokens = estimateTokens(request.prompt),
                    outputTokens = estimateTokens(text),
                    totalTokens = estimateTokens(request.prompt) + estimateTokens(text)
                )
            )
        } catch (e: Exception) {
            logger.error("Error generating response from Gemini", e)
            throw e
        }
    }

    override suspend fun generateAppSchema(request: AppBuilderAiRequest): AppBuilderAiResponse {
        val systemPrompt = """
            You are an expert Android app architect. Analyze the user request and generate a structured app schema.
            Return ONLY valid JSON:
            {
                "appName": "string",
                "packageName": "string",
                "screens": [
                    {
                        "screenName": "string",
                        "screenType": "splash|form|list|grid|detail|checkout|confirmation",
                        "layout": "vertical_form|vertical_list|product_grid|centered|checkout_form",
                        "fields": [{"name": "string", "type": "text|email|password|number|boolean", "label": "string", "required": boolean}]
                    }
                ]
            }
        """.trimIndent()

        val fullPrompt = """$systemPrompt
            
            App Request: ${request.prompt}
            Context: ${json.encodeToString(AppContext.serializer(), request.appContext)}
        """.trimIndent()

        return try {
            val response = model.generateContent(
                content {
                    text(fullPrompt)
                }
            )

            val text = response.text ?: "{}"
            val schema = try {
                json.decodeFromString<Map<String, Any>>(text)
            } catch (e: Exception) {
                logger.warn("Failed to parse schema JSON, returning partial", e)
                mapOf(
                    "appName" to (request.appContext.appName.ifEmpty { "GeneratedApp" }),
                    "packageName" to (request.appContext.packageName.ifEmpty { "com.example.app" })
                )
            }

            AppBuilderAiResponse(
                schema = schema,
                screens = parseScreens(schema),
                confidence = calculateConfidence(schema),
                suggestions = generateSuggestions(schema, request),
                model = "gemini-1.5-pro"
            )
        } catch (e: Exception) {
            logger.error("Error generating app schema", e)
            throw e
        }
    }

    override suspend fun generateCode(request: AppBuilderAiRequest): GeneratedCodeResponse {
        val prompt = """
            Generate production-ready Kotlin/Compose code for an Android screen.
            Requirements:
            - Use Jetpack Compose (Material3)
            - Include state management with ViewModel
            - Proper error handling
            - Accessibility support
            - Follow Android best practices
            
            Screen Request: ${request.prompt}
            
            Return the complete Kotlin file with proper package declarations and imports.
        """.trimIndent()

        return try {
            val response = model.generateContent(
                content {
                    text(prompt)
                }
            )

            val code = response.text ?: ""

            GeneratedCodeResponse(
                code = code,
                language = "kotlin",
                type = detectCodeType(code),
                fileStructure = mapOf("screen" to code)
            )
        } catch (e: Exception) {
            logger.error("Error generating code", e)
            throw e
        }
    }

    override suspend fun analyzeAndOptimize(request: AppBuilderAiRequest): OptimizationResponse {
        val prompt = """
            Analyze this Android code and provide optimization suggestions:
            
            ${request.prompt}
            
            Provide optimization suggestions, performance tips, and security improvements.
        """.trimIndent()

        return try {
            val response = model.generateContent(
                content {
                    text(prompt)
                }
            )

            val text = response.text ?: ""

            OptimizationResponse(
                optimizations = text.split("\n").filter { it.isNotBlank() }.take(5),
                performanceTips = emptyList(),
                securitySuggestions = emptyList()
            )
        } catch (e: Exception) {
            logger.error("Error analyzing code", e)
            throw e
        }
    }

    override fun getProviderName(): String = "Gemini 1.5 Pro"

    override fun isAvailable(): Boolean = true

    private fun buildPrompt(request: AiRequest): String {
        return """
            ${request.context.entries.joinToString("\n") { (k, v) -> "Context: $k = $v" }}
            
            Prompt: ${request.prompt}
        """.trimIndent()
    }

    private fun parseScreens(schema: Map<String, Any>): List<ScreenGenerationResult> {
        val screens = mutableListOf<ScreenGenerationResult>()
        val screensList = (schema["screens"] as? List<*>) ?: return screens

        screensList.forEach { screenObj ->
            if (screenObj is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val screen = screenObj as Map<String, Any>
                screens.add(
                    ScreenGenerationResult(
                        screenName = screen["screenName"] as? String ?: "UnknownScreen",
                        screenType = screen["screenType"] as? String ?: "form",
                        fields = (screen["fields"] as? List<*>)?.mapNotNull {
                            if (it is Map<*, *>) (it as Map<String, String>) else null
                        } ?: emptyList(),
                        layout = screen["layout"] as? String ?: "vertical_form"
                    )
                )
            }
        }

        return screens
    }

    private fun calculateConfidence(schema: Map<String, Any>): Double {
        val hasAppName = schema.containsKey("appName")
        val hasScreens = (schema["screens"] as? List<*>)?.isNotEmpty() == true
        val hasNavigation = schema.containsKey("navigationFlow")

        return when {
            hasAppName && hasScreens && hasNavigation -> 0.95
            hasAppName && hasScreens -> 0.85
            else -> 0.70
        }
    }

    private fun generateSuggestions(schema: Map<String, Any>, request: AppBuilderAiRequest): List<String> {
        val suggestions = mutableListOf<String>()

        if ((schema["screens"] as? List<*>)?.isEmpty() != false) {
            suggestions.add("Consider adding more screens for better UX flow")
        }

        if (!schema.containsKey("navigationFlow")) {
            suggestions.add("Add navigation flow definition for smooth screen transitions")
        }

        suggestions.add("Use dependency injection (Hilt) for better code organization")
        suggestions.add("Implement error handling and loading states")

        return suggestions
    }

    private fun detectCodeType(code: String): String {
        return when {
            code.contains("ViewModel") -> "screen"
            code.contains("NavHost") -> "navigation"
            code.contains("State(") -> "state"
            else -> "full_app"
        }
    }

    private fun estimateTokens(text: String): Int {
        return (text.split("\\s+".toRegex()).size * 1.3).toInt()
    }
}
