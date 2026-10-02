package com.ashiyerrc.appbuilder.ai.engine

import com.ashiyerrc.appbuilder.ai.domain.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

enum class ScreenIntent {
    AUTHENTICATION,
    FORM_INPUT,
    LIST_DISPLAY,
    GRID_DISPLAY,
    DETAIL_VIEW,
    CHECKOUT,
    CONFIRMATION,
    UNKNOWN
}

data class ParsedPrompt(
    val intent: ScreenIntent,
    val screenName: String,
    val fields: List<Map<String, String>>,
    val layoutHint: String,
    val confidence: Double,
    val additionalScreens: List<ScreenGenerationResult> = emptyList()
)

class PromptToSchemaEngine {

    private val authKeywords = listOf("login", "signin", "auth", "password", "email", "register", "signup")
    private val formKeywords = listOf("form", "input", "submit", "validate", "profile", "settings")
    private val listKeywords = listOf("list", "feed", "items", "timeline", "scroll", "rows")
    private val gridKeywords = listOf("grid", "gallery", "products", "cards", "shop", "store")
    private val checkoutKeywords = listOf("checkout", "payment", "order", "cart", "shipping")
    private val confirmationKeywords = listOf("confirmation", "success", "thank you", "completed")

    fun parsePrompt(prompt: String): ParsedPrompt {
        val normalized = prompt.lowercase()
        val words = normalized.split(Regex("\\W+"))

        val scoreMap = mapOf(
            ScreenIntent.AUTHENTICATION to score(words, authKeywords),
            ScreenIntent.FORM_INPUT to score(words, formKeywords),
            ScreenIntent.LIST_DISPLAY to score(words, listKeywords),
            ScreenIntent.GRID_DISPLAY to score(words, gridKeywords),
            ScreenIntent.CHECKOUT to score(words, checkoutKeywords),
            ScreenIntent.CONFIRMATION to score(words, confirmationKeywords)
        )

        val (intent, confidence) = scoreMap.maxByOrNull { it.value }
            ?.let { it.key to it.value }
            ?: (ScreenIntent.UNKNOWN to 0.0)

        val screenName = extractScreenName(prompt, intent)
        val fields = extractFields(prompt, intent)
        val layoutHint = layoutHintFor(intent)

        logger.info("Parsed prompt: intent=$intent, screen=$screenName, confidence=$confidence")

        return ParsedPrompt(
            intent = intent,
            screenName = screenName,
            fields = fields,
            layoutHint = layoutHint,
            confidence = confidence
        )
    }

    fun generateAppSchema(parsed: ParsedPrompt, appContext: AppContext): AppBuilderAiResponse {
        val screens = mutableListOf<ScreenGenerationResult>()

        // Main screen from parsed prompt
        screens.add(
            ScreenGenerationResult(
                screenName = parsed.screenName,
                screenType = parsed.intent.name.lowercase(),
                fields = parsed.fields,
                layout = parsed.layoutHint
            )
        )

        // Add complementary screens based on intent
        when (parsed.intent) {
            ScreenIntent.AUTHENTICATION -> {
                screens.add(
                    ScreenGenerationResult(
                        screenName = "HomeScreen",
                        screenType = "list",
                        fields = listOf(mapOf("name" to "title", "type" to "text", "label" to "Welcome")),
                        layout = "vertical_list"
                    )
                )
            }
            ScreenIntent.GRID_DISPLAY -> {
                screens.add(
                    ScreenGenerationResult(
                        screenName = "DetailScreen",
                        screenType = "detail",
                        fields = listOf(mapOf("name" to "description", "type" to "text", "label" to "Description")),
                        layout = "vertical_detail"
                    )
                )
            }
            ScreenIntent.CHECKOUT -> {
                screens.add(
                    ScreenGenerationResult(
                        screenName = "ConfirmationScreen",
                        screenType = "confirmation",
                        fields = emptyList(),
                        layout = "centered_confirmation"
                    )
                )
            }
            else -> {}
        }

        val schema = mapOf(
            "appName" to (appContext.appName.ifEmpty { "GeneratedApp" }),
            "packageName" to (appContext.packageName.ifEmpty { "com.example.app" }),
            "version" to "1.0.0",
            "minSdk" to 24,
            "targetSdk" to 34,
            "theme" to appContext.designSystem,
            "screens" to screens.map { s ->
                mapOf(
                    "screenName" to s.screenName,
                    "screenType" to s.screenType,
                    "layout" to s.layout,
                    "fields" to s.fields
                )
            }
        )

        return AppBuilderAiResponse(
            schema = schema,
            screens = screens,
            confidence = parsed.confidence,
            suggestions = generateSuggestions(parsed, screens)
        )
    }

    private fun score(words: List<String>, keywords: List<String>): Double {
        val matches = words.count { it in keywords }
        return if (words.isNotEmpty()) matches.toDouble() / words.size else 0.0
    }

    private fun extractScreenName(prompt: String, intent: ScreenIntent): String {
        val customMatch = Regex("screen\\s+(?:called|named|for)\\s+(\\w+)", RegexOption.IGNORE_CASE)
            .find(prompt)?.groupValues?.get(1)

        return customMatch ?: when (intent) {
            ScreenIntent.AUTHENTICATION -> "LoginScreen"
            ScreenIntent.FORM_INPUT -> "FormScreen"
            ScreenIntent.LIST_DISPLAY -> "ListScreen"
            ScreenIntent.GRID_DISPLAY -> "GridScreen"
            ScreenIntent.CHECKOUT -> "CheckoutScreen"
            ScreenIntent.CONFIRMATION -> "ConfirmationScreen"
            else -> "GeneratedScreen"
        }
    }

    private fun extractFields(prompt: String, intent: ScreenIntent): List<Map<String, String>> {
        return when (intent) {
            ScreenIntent.AUTHENTICATION -> listOf(
                mapOf("name" to "email", "type" to "email", "label" to "Email", "required" to "true"),
                mapOf("name" to "password", "type" to "password", "label" to "Password", "required" to "true")
            )
            ScreenIntent.FORM_INPUT -> {
                val fieldPattern = Regex("(\\w+)\\s+field|input\\s+for\\s+(\\w+)", RegexOption.IGNORE_CASE)
                fieldPattern.findAll(prompt).map { match ->
                    val fieldName = match.groupValues[1].ifBlank { match.groupValues[2] }.lowercase()
                    mapOf(
                        "name" to fieldName,
                        "type" to "text",
                        "label" to fieldName.replaceFirstChar { it.uppercase() },
                        "required" to "true"
                    )
                }.toList()
            }
            ScreenIntent.CHECKOUT -> listOf(
                mapOf("name" to "fullName", "type" to "text", "label" to "Full Name", "required" to "true"),
                mapOf("name" to "email", "type" to "email", "label" to "Email", "required" to "true"),
                mapOf("name" to "address", "type" to "text", "label" to "Address", "required" to "true"),
                mapOf("name" to "zipCode", "type" to "text", "label" to "ZIP Code", "required" to "true")
            )
            ScreenIntent.LIST_DISPLAY -> listOf(
                mapOf("name" to "title", "type" to "text", "label" to "Title", "required" to "false")
            )
            ScreenIntent.GRID_DISPLAY -> listOf(
                mapOf("name" to "productName", "type" to "text", "label" to "Product", "required" to "true"),
                mapOf("name" to "price", "type" to "number", "label" to "Price", "required" to "true")
            )
            else -> listOf(
                mapOf("name" to "title", "type" to "text", "label" to "Title", "required" to "false")
            )
        }
    }

    private fun layoutHintFor(intent: ScreenIntent): String = when (intent) {
        ScreenIntent.AUTHENTICATION -> "vertical_form"
        ScreenIntent.FORM_INPUT -> "vertical_form"
        ScreenIntent.LIST_DISPLAY -> "vertical_list"
        ScreenIntent.GRID_DISPLAY -> "product_grid"
        ScreenIntent.CHECKOUT -> "checkout_form"
        ScreenIntent.CONFIRMATION -> "centered_confirmation"
        else -> "centered"
    }

    private fun generateSuggestions(
        parsed: ParsedPrompt,
        screens: List<ScreenGenerationResult>
    ): List<String> {
        val suggestions = mutableListOf<String>()

        if (parsed.confidence < 0.7) {
            suggestions.add("Consider providing more details for better schema generation")
        }

        if (screens.size == 1) {
            suggestions.add("Consider adding navigation between screens for better UX")
        }

        suggestions.add("Use Hilt for dependency injection")
        suggestions.add("Implement proper error handling and loading states")
        suggestions.add("Add accessibility support with content descriptions")

        return suggestions.take(5)
    }
}
