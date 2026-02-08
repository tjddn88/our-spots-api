package com.mrseong.picks.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@ConditionalOnWebApplication
class WebMvcConfig(
    private val jwtInterceptor: JwtInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(jwtInterceptor)
            .addPathPatterns("/api/places/**", "/api/map/markers/refresh")
            .excludePathPatterns("/api/auth/**")
    }
}
