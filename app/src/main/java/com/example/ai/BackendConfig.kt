package com.example.ai

import com.example.BuildConfig

object BackendConfig {
    val baseUrl: String
        get() {
            val configUrl = BuildConfig.JARVIS_BACKEND_URL
            return if (!configUrl.isNullOrBlank()) {
                configUrl.trim().removeSuffix("/")
            } else {
                "https://jarvis-ai-fn5x.onrender.com"
            }
        }

    val deviceToken: String
        get() {
            val token = BuildConfig.JARVIS_DEVICE_TOKEN
            return if (!token.isNullOrBlank()) {
                token.trim()
            } else {
                "jarvis_secure_personal_token_12345"
            }
        }
}
