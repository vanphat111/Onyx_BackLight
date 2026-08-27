package vn.vanphat111.onyxbacklight

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

class CallSettingsActivity : AppCompatActivity() {

    private val frame1Colors = IntArray(4) { Color.RED }
    private val frame2Colors = IntArray(4) { Color.BLUE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_settings)

        val prefs = getSharedPreferences("OnyxPrefs", Context.MODE_PRIVATE)

        val switchEnable = findViewById<Switch>(R.id.switchCallLed)
        val seekSpeed = findViewById<SeekBar>(R.id.seekSpeed)
        val seekBrightness = findViewById<SeekBar>(R.id.seekBrightness)
        val tvSpeedVal = findViewById<TextView>(R.id.tvSpeedVal)
        val tvBrightVal = findViewById<TextView>(R.id.tvBrightVal)
        val btnDemoCall = findViewById<Button>(R.id.btnDemoCall)
        val btnTurnOff = findViewById<Button>(R.id.btnTurnOff)

        val f1Views = arrayOf(
            findViewById<View>(R.id.f1_led1),
            findViewById<View>(R.id.f1_led2),
            findViewById<View>(R.id.f1_led3),
            findViewById<View>(R.id.f1_led4)
        )

        val f2Views = arrayOf(
            findViewById<View>(R.id.f2_led1),
            findViewById<View>(R.id.f2_led2),
            findViewById<View>(R.id.f2_led3),
            findViewById<View>(R.id.f2_led4)
        )

        switchEnable.isChecked = prefs.getBoolean("call_led_enabled", true)

        val currentSpeed = prefs.getInt("call_flash_speed", 150)
        seekSpeed.progress = currentSpeed
        tvSpeedVal.text = "Flash Speed: $currentSpeed ms"

        val currentBright = prefs.getInt("call_brightness", 255)
        seekBrightness.progress = currentBright
        tvBrightVal.text = "Call Brightness: $currentBright"

        loadFrameColors(prefs.getString("call_frame_1", "FRAME 0xFF0000 0xFF0000 0x000000 0x000000")!!, frame1Colors)
        loadFrameColors(prefs.getString("call_frame_2", "FRAME 0x000000 0x000000 0x0000FF 0x0000FF")!!, frame2Colors)

        updateCircleViews(f1Views, frame1Colors)
        updateCircleViews(f2Views, frame2Colors)

        f1Views.forEachIndexed { index, view ->
            view.setOnClickListener {
                openColorPicker("Frame 1 - LED ${index + 1}", "CallColorPicker_F1_$index") { newColor ->
                    frame1Colors[index] = newColor
                    updateCircleViews(f1Views, frame1Colors)
                    saveFrameCmd("call_frame_1", frame1Colors)
                }
            }
        }

        f2Views.forEachIndexed { index, view ->
            view.setOnClickListener {
                openColorPicker("Frame 2 - LED ${index + 1}", "CallColorPicker_F2_$index") { newColor ->
                    frame2Colors[index] = newColor
                    updateCircleViews(f2Views, frame2Colors)
                    saveFrameCmd("call_frame_2", frame2Colors)
                }
            }
        }

        switchEnable.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("call_led_enabled", isChecked).apply()
        }

        seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvSpeedVal.text = "Flash Speed: $progress ms"
                prefs.edit().putInt("call_flash_speed", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvBrightVal.text = "Call Brightness: $progress"
                prefs.edit().putInt("call_brightness", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnDemoCall.setOnClickListener {
            CallLedAnimator.start(this)
        }

        btnTurnOff.setOnClickListener {
            CallLedAnimator.stop(this)
        }
    }

    private fun updateCircleViews(views: Array<View>, colors: IntArray) {
        views.forEachIndexed { i, view ->
            view.backgroundTintList = ColorStateList.valueOf(colors[i])
        }
    }

    private fun loadFrameColors(cmd: String, targetArray: IntArray) {
        val parts = cmd.split(" ").filter { it.startsWith("0x", true) }
        for (i in 0 until minOf(parts.size, 4)) {
            try {
                val hex = parts[i].removePrefix("0x").removePrefix("0X")
                targetArray[i] = Color.parseColor("#$hex")
            } catch (_: Exception) {}
        }
    }

    private fun saveFrameCmd(key: String, colors: IntArray) {
        val cmd = buildString {
            append("FRAME")
            colors.forEach { color ->
                val hex = String.format("%06X", 0xFFFFFF and color)
                append(" 0x$hex")
            }
        }
        getSharedPreferences("OnyxPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString(key, cmd)
            .apply()
    }

    private fun openColorPicker(title: String, prefKey: String, onColorSelected: (Int) -> Unit) {
        ColorPickerDialog.Builder(this)
            .setTitle(title)
            .setPreferenceName(prefKey)
            .setPositiveButton("Select", ColorEnvelopeListener { envelope, _ ->
                onColorSelected(envelope.color)
            })
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .attachAlphaSlideBar(false)
            .attachBrightnessSlideBar(true)
            .setBottomSpace(12)
            .show()
    }
}