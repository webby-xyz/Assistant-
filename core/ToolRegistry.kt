package com.yourapp.assistant.core

import android.accessibilityservice.AccessibilityService
import android.content.Context
import com.yourapp.assistant.service.AssistantAccessibilityService

class ToolRegistry(private val ctx: Context) {

    // Yahan har nayi capability ek tool ban kar add hoti hai.
    val all: List<Tool> = listOf(

        ToolImpl("open_app", "Koi bhi installed app kholo", "app khol raha hoon",
            listOf(ToolParam("package", "string", "App ka package name, jaise com.whatsapp")),
            { SystemTools.openApp(ctx, it["package"]!!) }),

        ToolImpl("tap", "Screen par coordinates par tap karo", "tap kar raha hoon",
            listOf(ToolParam("x", "integer", "X coordinate"), ToolParam("y", "integer", "Y coordinate")),
            { AssistantAccessibilityService.instance?.tap(it["x"]!!.toInt(), it["y"]!!.toInt()); "Tapped" }),

        ToolImpl("click_text", "Screen par likha text dhoondh kar us par click karo", "button dhoondh raha hoon",
            listOf(ToolParam("text", "string", "Click karne wala text")),
            { AssistantAccessibilityService.instance?.clickText(it["text"]!!) ?: "ERROR: Service off hai" }),

        ToolImpl("type_text", "Kisi text field mein type karo", "type kar raha hoon",
            listOf(ToolParam("text", "string", "Type karne wala text")),
            { AssistantAccessibilityService.instance?.typeText(it["text"]!!) ?: "ERROR: Service off hai" }),

        ToolImpl("swipe", "Screen par swipe karo", "swipe kar raha hoon",
            listOf(ToolParam("x1", "integer", ""), ToolParam("y1", "integer", ""),
                   ToolParam("x2", "integer", ""), ToolParam("y2", "integer", "")),
            { AssistantAccessibilityService.instance?.swipe(it["x1"]!!.toInt(), it["y1"]!!.toInt(), it["x2"]!!.toInt(), it["y2"]!!.toInt()); "Swiped" }),

        ToolImpl("scroll", "Screen scroll karo - direction up ya down", "scroll kar raha hoon",
            listOf(ToolParam("direction", "string", "up ya down")),
            { AssistantAccessibilityService.instance?.scroll(it["direction"]!!) ?: "ERROR: Service off hai" }),

        ToolImpl("press_back", "Back button dabao", "back dabata hoon",
            emptyList(),
            { AssistantAccessibilityService.instance?.pressGlobal(AccessibilityService.GLOBAL_ACTION_BACK) ?: "ERROR" }),

        ToolImpl("press_home", "Home button dabao", "home ja raha hoon",
            emptyList(),
            { AssistantAccessibilityService.instance?.pressGlobal(AccessibilityService.GLOBAL_ACTION_HOME) ?: "ERROR" }),

        ToolImpl("send_sms", "SMS bhejo", "SMS bhej raha hoon",
            listOf(ToolParam("number", "string", "Phone number"), ToolParam("message", "string", "SMS ka text")),
            { SystemTools.sendSms(ctx, it["number"]!!, it["message"]!!) }),

        ToolImpl("make_call", "Phone call karo", "call kar raha hoon",
            listOf(ToolParam("number", "string", "Phone number")),
            { SystemTools.makeCall(ctx, it["number"]!!) }),

        ToolImpl("set_volume", "Volume set karo, 0 se 100", "volume set kar raha hoon",
            listOf(ToolParam("level", "integer", "0 se 100")),
            { SystemTools.setVolume(ctx, it["level"]!!.toInt()) }),

        ToolImpl("set_brightness", "Screen brightness set karo, 0 se 100 percent", "brightness set kar raha hoon",
            listOf(ToolParam("percent", "integer", "0 se 100")),
            { SystemTools.setBrightness(ctx, it["percent"]!!.toInt()) }),

        ToolImpl("flashlight", "Torch on ya off karo", "torch kar raha hoon",
            listOf(ToolParam("on", "boolean", "true ya false")),
            { SystemTools.toggleFlashlight(ctx, it["on"]!!.toBoolean()) }),

        ToolImpl("vibrate", "Phone vibrate karo", "vibrate kar raha hoon",
            listOf(ToolParam("ms", "integer", "Milliseconds, jaise 500")),
            { SystemTools.vibrate(ctx, it["ms"]!!.toLong()) }),

        ToolImpl("battery", "Battery ki status batao", "battery check kar raha hoon",
            emptyList(),
            { SystemTools.batteryLevel(ctx) }),

        ToolImpl("run_shell", "Shell command chalao (root ke bina limited)", "shell command chala raha hoon",
            listOf(ToolParam("cmd", "string", "Command")),
            { ShellExecutor.run(it["cmd"]!!) })
    )
}
