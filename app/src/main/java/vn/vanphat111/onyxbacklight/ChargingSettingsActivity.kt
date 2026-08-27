package vn.vanphat111.onyxbacklight

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

class ChargingSettingsActivity : AppCompatActivity() {
    private val TAG = "OnyxLED_ChargeSettings"
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charging_settings)

        prefs = getSharedPreferences("OnyxPrefs", Context.MODE_PRIVATE)

        setupLedStage("Low", listOf(R.id.low1, R.id.low2, R.id.low3, R.id.low4), "low", "charge_low_cmd", "0xFF0000")
        setupLedStage("Medium", listOf(R.id.med1, R.id.med2, R.id.med3, R.id.med4), "med", "charge_med_cmd", "0xFFFF00")
        setupLedStage("Full", listOf(R.id.full1, R.id.full2, R.id.full3, R.id.full4), "full", "charge_full_cmd", "0x00FF00")

        setupBrightnessControl()
        setupActionButtons()
    }

    private fun parseHex(hex: String): Int {
        return try {
            android.graphics.Color.parseColor(hex.replace("0x", "#"))
        } catch (e: Exception) {
            android.graphics.Color.WHITE
        }
    }

    private fun setupLedStage(
        stageName: String,
        viewIds: List<Int>,
        prefPrefix: String,
        cmdPrefKey: String,
        defaultHex: String
    ) {
        val colors = Array(4) { i -> prefs.getString("charge_${prefPrefix}_c${i+1}", defaultHex)!! }

        fun updateUIAndHardware() {
            for (i in 0..3) {
                val view = findViewById<View>(viewIds[i])
                view.backgroundTintList = android.content.res.ColorStateList.valueOf(parseHex(colors[i]))
            }

            val cmd = "FRAME ${colors[0]} ${colors[1]} ${colors[2]} ${colors[3]}"
            Log.d(TAG, "Stage $stageName Updated: $cmd")

            prefs.edit().apply {
                putString("charge_${prefPrefix}_c1", colors[0])
                putString("charge_${prefPrefix}_c2", colors[1])
                putString("charge_${prefPrefix}_c3", colors[2])
                putString("charge_${prefPrefix}_c4", colors[3])
                putString(cmdPrefKey, cmd)
                apply()
            }

            val brightness = prefs.getInt("charging_brightness", 128)
            kotlin.concurrent.thread {
                val led = LedController()
                led.sendCommand("BRIGHTNESS $brightness")
                led.sendCommand(cmd)
            }
        }

        for (i in 0..3) {
            val view = findViewById<View>(viewIds[i])
            view.backgroundTintList = android.content.res.ColorStateList.valueOf(parseHex(colors[i]))

            view.setOnClickListener {
                ColorPickerDialog.Builder(this)
                    .setTitle("$stageName - LED ${i+1}")
                    .setPreferenceName("ColorPicker_${prefPrefix}_$i")
                    .setPositiveButton("Select", ColorEnvelopeListener { envelope, _ ->
                        val c = envelope.color
                        val hex = String.format("0x%02X%02X%02X",
                            android.graphics.Color.red(c),
                            android.graphics.Color.green(c),
                            android.graphics.Color.blue(c))

                        colors[i] = hex
                        updateUIAndHardware()
                    })
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .attachAlphaSlideBar(false)
                    .attachBrightnessSlideBar(true)
                    .setBottomSpace(12)
                    .show()
            }
        }
    }

    private fun setupBrightnessControl() {
        val seekBrightness = findViewById<SeekBar>(R.id.seekChargeBrightness)
        val lblBrightness = findViewById<TextView>(R.id.lblChargeBrightness)

        val savedBrightness = prefs.getInt("charging_brightness", 128)
        seekBrightness.progress = savedBrightness
        lblBrightness.text = "Charging Brightness: $savedBrightness"

        seekBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                lblBrightness.text = "Charging Brightness: $progress"
                if (fromUser) {
                    Log.d(TAG, "Charging Brightness Changed: $progress")
                    prefs.edit().putInt("charging_brightness", progress).apply()
                    LedController().sendCommand("BRIGHTNESS $progress")
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupActionButtons() {
        findViewById<Button>(R.id.btnDemoCharge).setOnClickListener {
            Log.d(TAG, "Demo Charge Clicked")
            (it as Button).text = "Running Color Wave..."

            kotlin.concurrent.thread {
                val led = LedController()
                led.sendCommand("BRIGHTNESS 200")

                val colors = listOf(
                    0xFF0000, 0xFFFF00, 0x00FF00, 0x00FFFF, 0x0000FF, 0xFF00FF, 0xFF0000
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

                val totalSteps = 150
                for (step in 0..totalSteps) {
                    val baseProgress = step.toFloat() / 25f
                    val p1 = baseProgress
                    val p2 = baseProgress + 0.25f
                    val p3 = baseProgress + 0.50f
                    val p4 = baseProgress + 0.75f

                    val hex1 = toHex(getColorAt(p1))
                    val hex2 = toHex(getColorAt(p2))
                    val hex3 = toHex(getColorAt(p3))
                    val hex4 = toHex(getColorAt(p4))

                    led.sendCommand("FRAME $hex1 $hex2 $hex3 $hex4")
                    Thread.sleep(30)
                }

                runOnUiThread { it.text = "Demo Effect" }
                LedStateManager.restoreBaseState(applicationContext)
            }
        }

        findViewById<Button>(R.id.btnTurnOff).setOnClickListener {
            Log.d(TAG, "Turn Off Clicked")
            kotlin.concurrent.thread { LedController().sendCommand("OFF") }
        }
    }
}