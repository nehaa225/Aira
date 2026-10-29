package com.aira.accessibility.agent

class AiraToolRegistry {

    private val tools = mutableMapOf<String, AiraTool>()

    fun register(tool: AiraTool) {
        tools[tool.name] = tool
    }

    fun getTool(name: String): AiraTool? {
        return tools[name]
    }

    fun hasTool(name: String): Boolean {
        return tools.containsKey(name)
    }

    fun getAllTools(): List<AiraTool> {
        return tools.values.toList()
    }

    fun getToolDescriptions(): String {
        if (tools.isEmpty()) {
            return "No tools are currently available."
        }

        return tools.values.joinToString("\n") { tool ->
            "${tool.name}: ${tool.description}"
        }
    }
}