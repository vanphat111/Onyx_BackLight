package vn.vanphat111.onyxbacklight

import android.content.Context
import android.util.Log
import kotlin.concurrent.thread

object CallLedAnimator {
    private const val TAG = "OnyxLED_CallAnim"
    @Volatile var isRinging = false
        private set
    private var callThread: Thread? = null

    fun start(context: Context) {
        if (isRinging) {
            Log.d(TAG, "start() called but already ringing, ignoring.")
            return
        }

        val prefs = context.getSharedPreferences("OnyxPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("call_led_enabled", true)) {
            Log.d(TAG, "start() called but call_led_enabled is FALSE.")
            return
        }

        isRinging = true
        val brightness = prefs.getInt("call_brightness", 255)
        val speedMs = prefs.getInt("call_flash_speed", 150)
        val frame1 = prefs.getString("call_frame_1", "FRAME 0xFF0000 0xFF0000 0x000000 0x000000")!!
        val frame2 = prefs.getString("call_frame_2", "FRAME 0x000000 0x000000 0x0000FF 0x0000FF")!!

        Log.d(TAG, "Call Animation STARTED: Speed=${speedMs}ms, Brightness=$brightness")

        callThread = thread(start = true) {
            val led = LedController()
            led.resetForStatic()
            led.sendCommand("BRIGHTNESS $brightness")

            var toggle = true
            while (isRinging) {
                led.sendCommand(if (toggle) frame1 else frame2)
                toggle = !toggle

                try {
                    Thread.sleep(speedMs.toLong())
                } catch (e: InterruptedException) {
                    Log.d(TAG, "Animation thread interrupted.")
                    break
                }
            }

            Log.d(TAG, "Call Animation loop ENDED. Restoring base state...")
            LedStateManager.restoreBaseState(context)
        }
    }

    fun stop(context: Context) {
        if (!isRinging) return
        Log.d(TAG, "Call Animation STOPPED requested.")
        isRinging = false
        callThread?.interrupt()
        callThread = null
    }
}