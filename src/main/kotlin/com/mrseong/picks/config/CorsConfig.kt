package com.mrseong.picks.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
@ConditionalOnWebApplication
class CorsConfig(
    @Value("\${app.cors.allowed-origins:}") private val allowedOriginsRaw: String
) {

    @Bean
    fun corsFilter(): CorsFilter {
        val origins = allowedOriginsRaw
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val config = CorsConfiguration().apply {
            allowedOrigins = origins
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }

        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }

        return CorsFilter(source)
    }
}
