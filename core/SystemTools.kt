package com.yourapp.assistant.core

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.telephony.SmsManager

object SystemTools {

    fun setVolume(ctx: Context, level: Int): String {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val v = level.coerceIn(0, max)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0)
        return "Volume $v / $max kar diya"
    }

    fun setBrightness(ctx: Context, percent: Int): String {
        val p = percent.coerceIn(0, 100)
        val value = (255 * p / 100).coerceIn(1, 255)
        Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
        return "Brightness $p% kar di"
    }

    fun toggleFlashlight(ctx: Context, on: Boolean): String {
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return "ERROR: Flash nahi mila"
        cm.setTorchMode(id, on)
        return if (on) "Torch ON" else "Torch OFF"
    }

    fun vibrate(ctx: Context, ms: Long): String {
        val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        return "${ms}ms ke liye vibrate kiya"
    }

    fun batteryLevel(ctx: Context): String {
        val b = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return "Battery $level% hai (${level * 100 / scale})" .ifEmpty { "Battery pata nahi chali" }
    }

    fun sendSms(ctx: Context, number: String, message: String): String {
        SmsManager.getDefault().sendTextMessage(number, null, message, null, null)
        return "SMS $number ko bhej diya"
    }

    fun makeCall(ctx: Context, number: String): String {
        val i = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        return "$number ko call kar raha hoon"
    }

    fun openApp(ctx: Context, packageName: String): String {
        val pm = ctx.packageManager
        val i = pm.getLaunchIntentForPackage(packageName)
            ?: return "ERROR: App nahi mila: $packageName"
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        return "$packageName khol diya"
    }
}
