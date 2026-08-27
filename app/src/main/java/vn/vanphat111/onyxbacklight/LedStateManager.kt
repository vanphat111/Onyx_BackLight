package vn.vanphat111.onyxbacklight

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import java.util.concurrent.Executors

object LedStateManager {
    private const val TAG = "OnyxLED_State"
    private val led = LedController()
    var isCameraBlocking = false
    private val executor = Executors.newSingleThreadExecutor()

    fun restoreBaseState(context: Context) {
        executor.submit {
            if (ColorWaveAnimator.isWaveActive()) {
                Log.d(TAG, "restoreBaseState skipped: RGB Wave is active")
                return@submit
            }
            if (CallLedAnimator.isRinging) {
                Log.d(TAG, "restoreBaseState skipped: Call Ringing is active")
                return@submit
            }

            val prefs = context.getSharedPreferences("OnyxPrefs", Context.MODE_PRIVATE)
            val isChargingEnabled = prefs.getBoolean("charging_enabled", false)

            if (isChargingEnabled) {
                val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val isPlugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
                val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

                val isCharging = isPlugged > 0 || status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                if (isCharging) {
                    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                    val batteryPct = if (scale > 0) (level * 100 / scale) else 50

                    val chargeBrightness = prefs.getInt("charging_brightness", 128)
                    val chargeColorCmd = when {
                        batteryPct <= 20 -> prefs.getString("charge_low_cmd", "FRAME 0xFF0000 0xFF0000 0xFF0000 0xFF0000")!!
                        batteryPct >= 90 -> prefs.getString("charge_full_cmd", "FRAME 0x00FF00 0x00FF00 0x00FF00 0x00FF00")!!
                        else -> prefs.getString("charge_med_cmd", "FRAME 0xFFFF00 0xFFFF00 0xFFFF00 0xFFFF00")!!
                    }

                    Log.d(TAG, "Restoring Charging State: $batteryPct% | Brightness: $chargeBrightness | CMD: $chargeColorCmd")
                    led.resetForStatic()
                    led.sendCommand("BRIGHTNESS $chargeBrightness")
                    led.sendCommand(chargeColorCmd)
                    return@submit
                }
            }

            Log.d(TAG, "Restoring Default State: LED OFF")
            led.resetAndOff()
        }
    }
}