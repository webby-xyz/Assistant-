package com.yourapp.assistant.ai

import android.content.Context
import com.yourapp.assistant.core.ToolRegistry
import com.yourapp.assistant.service.AssistantAccessibilityService
import kotlinx.coroutines.delay

class AgentLoop(private val context: Context, private val apiKey: String) {

    private val gemini = GeminiClient(apiKey)
    private val tools = ToolRegistry(context).all

    interface Listener {
        fun onAnnounce(text: String)      // "Ab volume set kar raha hoon..."
        fun onFinished(result: String)    // "Kaam ho gaya: ..."
        fun onError(error: String)        // Koi problem
    }

    suspend fun process(command: String, listener: Listener) {
        val system = """Tum ek Android phone assistant ho jo har command poori karta hai.
Screen ka XML (T:text, D:description, [tap@x,y]) diya jayega.
Rules:
1. Screen dekh kar decide karo kaunsa tool chahiye. Zaroorat ho to pehle app kholo ya scroll karo.
2. Har step ek hi tool call karo.
3. Jab task mukammal ho jaye tab Final text do - Roman Urdu mein ek chhota sa summary.
4. Agar command ke liye app kholna zaroori ho to open_app use karo.
5. Kabhi mat bolo "main nahi kar sakta" - koi aur tarika dhoondo."""

        listener.onAnnounce("Command mil gayi. Ab kaam shuru karta hoon.")
        var screen = AssistantAccessibilityService.instance?.getScreenText() ?: "(screen nahi mili)"

        var steps = 0
        while (steps < 15) {
            steps++
            when (val action = gemini.decide(system, screen, command, tools)) {
                is LlmAction.ToolCall -> {
                    val tool = tools.find { it.name == action.name }
                    if (tool == null) {
                        listener.onAnnounce("Unknown tool: ${action.name}")
                        continue
                    }
                    listener.onAnnounce("Ab ${tool.humanName}.")
                    val result = tool.execute(action.args)
                    listener.onAnnounce(result.take(120))
                    if (result.startsWith("ERROR")) {
                        listener.onError(result)
                        return
                    }
                    delay(700) // screen update hone ka intezar
                    screen = AssistantAccessibilityService.instance?.getScreenText() ?: "(screen nahi mili)"
                }
                is LlmAction.Final -> {
                    listener.onFinished(action.text)
                    return
                }
                is LlmAction.Error -> {
                    listener.onError(action.message)
                    return
                }
            }
        }
        listener.onFinished("Kaam zyada lamba ho gaya. Thodi aur detail de kar dobara try karo.")
    }
}
