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
    private lateinit var viewColorPreview: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charging_settings)

        prefs = getSharedPreferences("OnyxPrefs", Context.MODE_PRIVATE)
        viewColorPreview = findViewById(R.id.viewChargeColorPreview)

        val savedR = prefs.getInt("charge_color_r", 0)
        val savedG = prefs.getInt("charge_color_g", 255)
        val savedB = prefs.getInt("charge_color_b", 0)
        viewColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.rgb(savedR, savedG, savedB))

        setupColorPicker()
        setupBrightnessControl()
        setupActionButtons()
    }

    private fun setupColorPicker() {
        val led = LedController()

        fun saveAndPreviewColor(r: Int, g: Int, b: Int) {
            val hex = String.format("0x%02X%02X%02X", r, g, b)
            val command = "FRAME $hex $hex $hex $hex"

            prefs.edit()
                .putInt("charge_color_r", r)
                .putInt("charge_color_g", g)
                .putInt("charge_color_b", b)
                .putString("charging_color", command)
                .apply()

            viewColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.rgb(r, g, b))
            led.sendCommand(command)
        }

        findViewById<View>(R.id.chargeRed).setOnClickListener { saveAndPreviewColor(255, 0, 0) }
        findViewById<View>(R.id.chargeGreen).setOnClickListener { saveAndPreviewColor(0, 255, 0) }
        findViewById<View>(R.id.chargeYellow).setOnClickListener { saveAndPreviewColor(255, 255, 0) }
        findViewById<View>(R.id.chargeBlue).setOnClickListener { saveAndPreviewColor(0, 0, 255) }

        findViewById<Button>(R.id.btnPickChargeColor).setOnClickListener {
            ColorPickerDialog.Builder(this)
                .setTitle("Custom Charging Color")
                .setPreferenceName("ChargeColorPicker")
                .setPositiveButton("Select", ColorEnvelopeListener { envelope, _ ->
                    val c = envelope.color
                    saveAndPreviewColor(android.graphics.Color.red(c), android.graphics.Color.green(c), android.graphics.Color.blue(c))
                })
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .attachAlphaSlideBar(false)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .show()
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
                    prefs.edit().putInt("charging_brightness", progress).apply()
                    LedController().sendCommand("BRIGHTNESS $progress")
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupActionButtons() {
        val led = LedController()

        findViewById<Button>(R.id.btnDemoCharge).setOnClickListener {
            val color = prefs.getString("charging_color", "FRAME 0x00FF00 0x00FF00 0x00FF00 0x00FF00")!!
            val brightness = prefs.getInt("charging_brightness", 128)
            kotlin.concurrent.thread {
                led.sendCommand("BRIGHTNESS $brightness")
                led.sendCommand(color)
            }
        }

        findViewById<Button>(R.id.btnTurnOff).setOnClickListener {
            kotlin.concurrent.thread { led.sendCommand("OFF") }
        }
    }
}