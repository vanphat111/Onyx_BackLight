package vn.vanphat111.onyxbacklight

import android.content.Context
import kotlin.concurrent.thread

object CallLedAnimator {
    @Volatile var isRinging = false
        private set
    private var callThread: Thread? = null

    fun start(context: Context) {
        if (isRinging) return
        isRinging = true

        callThread = thread(start = true) {
            val prefs = context.getSharedPreferences("OnyxPrefs", Context.MODE_PRIVATE)

            if (!prefs.getBoolean("call_led_enabled", true)) {
                isRinging = false
                return@thread
            }

            val led = LedController()

            val brightness = prefs.getInt("call_brightness", 255)
            val speedMs = prefs.getInt("call_flash_speed", 150)

            val frame1 = prefs.getString("call_frame_1", "FRAME 0xFF0000 0xFF0000 0x000000 0x000000")!!
            val frame2 = prefs.getString("call_frame_2", "FRAME 0x000000 0x000000 0x0000FF 0x0000FF")!!

            led.sendCommand("RUN 0")
            led.sendCommand("EFFECT 0")
            led.sendCommand("TRIGGER none")
            led.sendCommand("BRIGHTNESS $brightness")

            var toggle = true

            while (isRinging) {
                if (toggle) {
                    led.sendCommand(frame1)
                } else {
                    led.sendCommand(frame2)
                }
                toggle = !toggle

                try {
                    Thread.sleep(speedMs.toLong())
                } catch (e: InterruptedException) {
                    break
                }
            }

            LedStateManager.restoreBaseState(context)
        }
    }

    fun stop(context: Context) {
        if (!isRinging) return
        isRinging = false
        callThread?.interrupt()
        callThread = null
    }
}