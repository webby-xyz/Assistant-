package com.yourapp.assistant

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.yourapp.assistant.ai.AgentLoop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var keyField: EditText
    private lateinit var inputField: EditText
    private lateinit var tts: TextToSpeech
    private lateinit var recognizer: SpeechRecognizer
    private var listening = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ---------- Simple UI (programmatic) ----------
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        root.addView(TextView(this).apply {
            text = "🤖 Mera Assistant"; textSize = 28f
        })
        status = TextView(this).apply {
            text = "Status: Ready"; textSize = 15f; setPadding(0, 24, 0, 24)
        }
        keyField = EditText(this).apply {
            hint = "Gemini API Key yahan daalo"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        keyField.setText(getSharedPreferences("prefs", MODE_PRIVATE).getString("api_key", ""))

        inputField = EditText(this).apply {
            hint = "Ya command yahan type karo... (jaise: whatsapp kholo)"
            setPadding(0, 16, 0, 16)
        }
        val runBtn = Button(this).apply { text = "▶ Command Chalao" }
        val micBtn = Button(this).apply { text = "🎤 Bolo (Voice Command)" }

        root.addView(status); root.addView(keyField); root.addView(inputField)
        root.addView(runBtn); root.addView(micBtn)
        setContentView(root)

        // ---------- Permissions ----------
        requestPermissions(
            arrayOf(android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.SEND_SMS,
                    android.Manifest.permission.CALL_PHONE,
                    android.Manifest.permission.CAMERA), 100)
        if (!Settings.System.canWrite(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")))
        }

        // ---------- TextToSpeech (bol ke batana) ----------
        tts = TextToSpeech(this) { s ->
            if (s == TextToSpeech.SUCCESS) tts.language = Locale("ur")
        }

        // ---------- SpeechRecognizer (voice sunna) ----------
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { recognizer.stopListening() }
            override fun onError(error: Int) {
                listening = false
                status.text = "Sunai nahi diya, dobara try karo (error $error)"
            }
            override fun onResults(results: Bundle?) {
                listening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text != null) {
                    status.text = "Aapne kaha: $text"
                    runCommand(text)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // ---------- Buttons ----------
        runBtn.setOnClickListener { runCommand(inputField.text.toString()) }
        micBtn.setOnClickListener { toggleListening() }

        status.text = "Step 1: Settings > Accessibility > Mera Assistant ON karo"
        Toast.makeText(this, "Pehle Accessibility ON karein!", Toast.LENGTH_LONG).show()
    }

    private fun toggleListening() {
        if (listening) { recognizer.stopListening(); listening = false; return }
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ur-PK")
        }
        listening = true
        status.text = "Sun raha hoon... boliye"
        recognizer.startListening(i)
    }

    private fun runCommand(cmd: String) {
        if (cmd.isBlank()) return
        val key = keyField.text.toString().trim()
        if (key.isEmpty()) {
            status.text = "Pehle Gemini API Key daalo!"
            return
        }
        getSharedPreferences("prefs", MODE_PRIVATE).edit().putString("api_key", key).apply()

        scope.launch {
            val loop = AgentLoop(this@MainActivity, key)
            loop.process(cmd, object : AgentLoop.Listener {
                override fun onAnnounce(text: String) {
                    speak(text)
                    status.text = text
                }
                override fun onFinished(result: String) {
                    speak("Kaam ho gaya. $result")
                    status.text = "✅ Done: $result"
                }
                override fun onError(error: String) {
                    speak("Problem aa gayi. $error")
                    status.text = "❌ Error: $error"
                }
            })
        }
    }

    private fun speak(text: String) {
        if (tts.isSpeaking) tts.stop()
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "msg")
    }

    override fun onDestroy() {
        scope.cancel()
        recognizer.destroy()
        tts.shutdown()
        super.onDestroy()
    }
}
