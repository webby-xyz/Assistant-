package com.yourapp.assistant.ai

import com.yourapp.assistant.core.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

sealed class LlmAction {
    data class ToolCall(val name: String, val args: Map<String, String>) : LlmAction()
    data class Final(val text: String) : LlmAction()
    data class Error(val message: String) : LlmAction()
}

class GeminiClient(private val apiKey: String) {

    private val client = OkHttpClient()
    private val url = "https://generativelanguage.googleapis.com/v1beta" +
            "/models/gemini-2.0-flash:generateContent?key=$apiKey"

    private fun buildTools(tools: List<Tool>): JSONArray {
        val decls = JSONArray()
        tools.forEach { t ->
            val f = JSONObject()
            f.put("name", t.name)
            f.put("description", t.description)
            val props = JSONObject()
            val required = JSONArray()
            t.params.forEach { p ->
                props.put(p.name, JSONObject().put("type", p.type).put("description", p.description))
                if (p.required) required.put(p.name)
            }
            f.put("parameters", JSONObject()
                .put("type", "object")
                .put("properties", props)
                .put("required", required))
            decls.put(f)
        }
        return JSONArray().put(JSONObject().put("functionDeclarations", decls))
    }

    suspend fun decide(system: String, screen: String, command: String, tools: List<Tool>): LlmAction =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                body.put("systemInstruction", JSONObject().put("parts",
                    JSONArray().put(JSONObject().put("text", system))))
                body.put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts",
                    JSONArray().put(JSONObject().put("text", "SCREEN:\n$screen\n\nUSER COMMAND:\n$command")))))
                body.put("tools", buildTools(tools))
                body.put("toolConfig", JSONObject().put("functionCallingConfig",
                    JSONObject().put("mode", "AUTO")))

                val request = Request.Builder()
                    .url(url)
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { resp ->
                    val text = resp.body?.string() ?: ""
                    if (!resp.isSuccessful) return@withContext LlmAction.Error("HTTP ${resp.code}: ${text.take(150)}")
                    val json = JSONObject(text)
                    val candidate = json.getJSONArray("candidates").getJSONObject(0)
                    val parts = candidate.getJSONObject("content").getJSONArray("parts")
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("functionCall")) {
                            val fc = part.getJSONObject("functionCall")
                            val args = mutableMapOf<String, String>()
                            fc.optJSONObject("args")?.let { a ->
                                a.keys().forEach { k -> args[k] = a.get(k).toString() }
                            }
                            return@withContext LlmAction.ToolCall(fc.getString("name"), args)
                        }
                        if (part.has("text")) {
                            return@withContext LlmAction.Final(part.getString("text"))
                        }
                    }
                    LlmAction.Error("Model se koi response nahi mila")
                }
            } catch (e: Exception) {
                LlmAction.Error(e.message ?: "Unknown error")
            }
        }
}
