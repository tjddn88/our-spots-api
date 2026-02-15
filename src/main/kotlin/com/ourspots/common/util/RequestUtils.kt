package com.ourspots.common.util

import jakarta.servlet.http.HttpServletRequest

object RequestUtils {

    fun getClientIp(request: HttpServletRequest): String {
        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) return xRealIp.trim()

        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) return xForwardedFor.split(",").first().trim()

        return request.remoteAddr ?: "unknown"
    }
}
