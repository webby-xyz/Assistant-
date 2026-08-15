package com.yourapp.assistant.core

object ShellExecutor {
    // NOTE: Bina root ke sirf chand commands chalengi (getprop, settings get...).
    // Rooted phone pe "su" available ho to almost sab chalega.
    fun run(cmd: String): String {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            p.waitFor()
            (out + err).trim().ifEmpty { "OK (exit ${p.exitValue()})" }
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }
}
