package com.ashiyerrc.appbuilder.feature.codegen

import com.squareup.kotlinpoet.*

data class TemplateRegistry(
    val templates: Map<String, ProjectTemplate> = emptyMap()
) {
    companion object {
        fun default(): TemplateRegistry {
            return TemplateRegistry(
                mapOf(
                    "starter" to starterTemplate(),
                    "ecommerce" to ecommerceTemplate()
                )
            )
        }

        private fun starterTemplate(): ProjectTemplate {
            return ProjectTemplate(
                id = "starter",
                name = "Starter App",
                description = "Basic app with auth flow",
                screens = listOf(
                    ScreenTemplate(
                        name = "SplashScreen",
                        type = "splash",
                        layout = "centered_image"
                    ),
                    ScreenTemplate(
                        name = "LoginScreen",
                        type = "form",
                        layout = "vertical_form"
                    ),
                    ScreenTemplate(
                        name = "HomeScreen",
                        type = "list",
                        layout = "vertical_list"
                    )
                )
            )
        }

        private fun ecommerceTemplate(): ProjectTemplate {
            return ProjectTemplate(
                id = "ecommerce",
                name = "E-commerce App",
                description = "Full e-commerce flow",
                screens = listOf(
                    ScreenTemplate(
                        name = "ProductListScreen",
                        type = "grid",
                        layout = "grid_layout"
                    ),
                    ScreenTemplate(
                        name = "ProductDetailScreen",
                        type = "detail",
                        layout = "vertical_detail"
                    ),
                    ScreenTemplate(
                        name = "CartScreen",
                        type = "list",
                        layout = "vertical_list"
                    ),
                    ScreenTemplate(
                        name = "CheckoutScreen",
                        type = "form",
                        layout = "vertical_form"
                    ),
                    ScreenTemplate(
                        name = "OrderConfirmationScreen",
                        type = "confirmation",
                        layout = "centered_confirmation"
                    )
                )
            )
        }
    }
}

data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val screens: List<ScreenTemplate>
)

data class ScreenTemplate(
    val name: String,
    val type: String,
    val layout: String,
    val components: List<ComponentTemplate> = emptyList()
)

data class ComponentTemplate(
    val id: String,
    val type: String,
    val label: String,
    val properties: Map<String, String> = emptyMap()
)
