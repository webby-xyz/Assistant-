package com.yourapp.assistant.core

// Har capability aik Tool hai. Naya tool = nayi command possible.
data class ToolParam(
    val name: String,
    val type: String,          // "string", "integer", "boolean"
    val description: String,
    val required: Boolean = true
)

interface Tool {
    val name: String           // LLM ko ye name dikhta hai
    val description: String    // LLM ko batao ke ye kya karta hai
    val humanName: String      // Bol ke batane ke liye ("app khol raha hoon")
    val params: List<ToolParam>
    suspend fun execute(args: Map<String, String>): String
}

class ToolImpl(
    override val name: String,
    override val description: String,
    override val humanName: String,
    override val params: List<ToolParam>,
    private val block: suspend (Map<String, String>) -> String
) : Tool {
    override suspend fun execute(args: Map<String, String>): String =
        try { block(args) }
        catch (e: Exception) { "ERROR: ${e.message ?: e.javaClass.simpleName}" }
}
