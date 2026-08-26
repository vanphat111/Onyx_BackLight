package vn.vanphat111.onyxbacklight

import android.content.Context
import kotlin.concurrent.thread

object ColorWaveAnimator {
    private var waveThread: Thread? = null
    @Volatile private var isRunning = false

    fun start(context: Context) {
        if (isRunning) return
        isRunning = true

        waveThread = thread(start = true) {
            val led = LedController()
            val prefs = context.getSharedPreferences("OnyxPrefs", Context.MODE_PRIVATE)
            val brightness = prefs.getInt("charging_brightness", 200)
            led.sendCommand("BRIGHTNESS $brightness")

            val colors = listOf(
                0xFF0000,
                0xFFFF00,
                0x00FF00,
                0x00FFFF,
                0x0000FF,
                0xFF00FF,
                0xFF0000
            )

            fun interpolateColor(c1: Int, c2: Int, fraction: Float): Int {
                val r1 = (c1 shr 16) and 0xFF
                val g1 = (c1 shr 8) and 0xFF
                val b1 = c1 and 0xFF
                val r2 = (c2 shr 16) and 0xFF
                val g2 = (c2 shr 8) and 0xFF
                val b2 = c2 and 0xFF
                val r = (r1 + (fraction * (r2 - r1))).toInt()
                val g = (g1 + (fraction * (g2 - g1))).toInt()
                val b = (b1 + (fraction * (b2 - b1))).toInt()
                return (r shl 16) or (g shl 8) or b
            }

            fun toHex(c: Int): String = String.format("0x%06X", c)

            fun getColorAt(pos: Float): Int {
                val safePos = pos % (colors.size - 1)
                val idx = safePos.toInt().coerceIn(0, colors.size - 2)
                val frac = safePos - idx
                return interpolateColor(colors[idx], colors[idx + 1], frac)
            }

            var step = 0f
            while (isRunning) {
                val baseProgress = step / 25f
                val p1 = baseProgress
                val p2 = baseProgress + 0.25f
                val p3 = baseProgress + 0.50f
                val p4 = baseProgress + 0.75f

                val hex1 = toHex(getColorAt(p1))
                val hex2 = toHex(getColorAt(p2))
                val hex3 = toHex(getColorAt(p3))
                val hex4 = toHex(getColorAt(p4))

                led.sendCommand("FRAME $hex1 $hex2 $hex3 $hex4")

                step += 1f
                try {
                    Thread.sleep(30)
                } catch (e: InterruptedException) {
                    break
                }
            }

            LedStateManager.restoreBaseState(context)
        }
    }

    fun stop(context: Context) {
        isRunning = false
        waveThread?.interrupt()
        waveThread = null
        LedStateManager.restoreBaseState(context)
    }

    fun isWaveActive(): Boolean = isRunning
}