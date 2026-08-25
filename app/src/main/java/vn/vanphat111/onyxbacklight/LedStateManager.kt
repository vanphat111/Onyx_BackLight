package vn.vanphat111.onyxbacklight

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

object LedStateManager {
    private val led = LedController()
    var isCameraBlocking = false

    fun restoreBaseState(context: Context) {
        val prefs = context.getSharedPreferences("OnyxPrefs", Context.MODE_PRIVATE)

        if (isCameraBlocking) {
            led.sendCommand("OFF")
            return
        }

        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val isChargingEnabled = prefs.getBoolean("charging_enabled", false)

        if (isCharging && isChargingEnabled) {
            val chargeColor = prefs.getString("charging_color", "FRAME 0x00FF00 0x00FF00 0x00FF00 0x00FF00")!!
            val chargeBrightness = prefs.getInt("charging_brightness", 128)

            led.sendCommand("BRIGHTNESS $chargeBrightness")
            led.sendCommand(chargeColor)
            return
        }

        led.sendCommand("OFF")
    }
}