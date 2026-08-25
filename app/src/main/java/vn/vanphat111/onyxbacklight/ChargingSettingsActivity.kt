package vn.vanphat111.onyxbacklight

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

class ChargingSettingsActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charging_settings)

        prefs = getSharedPreferences("OnyxPrefs", Context.MODE_PRIVATE)

        setupColors()
        setupBrightnessControl()
        setupActionButtons()
    }

    private fun setupColors() {
        val viewLow = findViewById<View>(R.id.colorLow)
        val viewMed = findViewById<View>(R.id.colorMed)
        val viewFull = findViewById<View>(R.id.colorFull)

        val colorLow = prefs.getInt("charge_low_color", android.graphics.Color.RED)
        val colorMed = prefs.getInt("charge_med_color", android.graphics.Color.YELLOW)
        val colorFull = prefs.getInt("charge_full_color", android.graphics.Color.GREEN)

        viewLow.backgroundTintList = android.content.res.ColorStateList.valueOf(colorLow)
        viewMed.backgroundTintList = android.content.res.ColorStateList.valueOf(colorMed)
        viewFull.backgroundTintList = android.content.res.ColorStateList.valueOf(colorFull)

        fun openPicker(title: String, prefKeyCmd: String, prefKeyColor: String, targetView: View) {
            ColorPickerDialog.Builder(this)
                .setTitle(title)
                .setPreferenceName(prefKeyColor)
                .setPositiveButton("Select", ColorEnvelopeListener { envelope, _ ->
                    val c = envelope.color
                    val r = android.graphics.Color.red(c)
                    val g = android.graphics.Color.green(c)
                    val b = android.graphics.Color.blue(c)

                    val hex = String.format("0x%02X%02X%02X", r, g, b)
                    val cmd = "FRAME $hex $hex $hex $hex"

                    prefs.edit()
                        .putInt(prefKeyColor, c)
                        .putString(prefKeyCmd, cmd)
                        .apply()

                    targetView.backgroundTintList = android.content.res.ColorStateList.valueOf(c)

                    val led = LedController()
                    val brightness = prefs.getInt("charging_brightness", 128)
                    kotlin.concurrent.thread {
                        led.sendCommand("BRIGHTNESS $brightness")
                        led.sendCommand(cmd)
                    }
                })
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .attachAlphaSlideBar(false)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .show()
        }

        viewLow.setOnClickListener { openPicker("Low Battery Color", "charge_low_cmd", "charge_low_color", viewLow) }
        viewMed.setOnClickListener { openPicker("Charging Color", "charge_med_cmd", "charge_med_color", viewMed) }
        viewFull.setOnClickListener { openPicker("Full Battery Color", "charge_full_cmd", "charge_full_color", viewFull) }
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
            LedStateManager.restoreBaseState(applicationContext)
        }

        findViewById<Button>(R.id.btnTurnOff).setOnClickListener {
            kotlin.concurrent.thread { LedController().sendCommand("OFF") }
        }
    }
}