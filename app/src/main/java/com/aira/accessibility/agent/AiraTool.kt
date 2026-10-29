package com.aira.accessibility.agent

interface AiraTool {

    val name: String

    val description: String

    suspend fun execute(
        parameters: Map<String, String>
    ): AiraToolResult
}

data class AiraToolResult(
    val success: Boolean,
    val message: String
)