package vn.vanphat111.onyxbacklight

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CallSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_settings)

        val prefs = getSharedPreferences("OnyxPrefs", Context.MODE_PRIVATE)

        val btnDemoCall = findViewById<Button>(R.id.btnDemoCall)
        val switchEnable = findViewById<Switch>(R.id.switchCallLed)
        val seekSpeed = findViewById<SeekBar>(R.id.seekSpeed)
        val seekBrightness = findViewById<SeekBar>(R.id.seekBrightness)
        val tvSpeedVal = findViewById<TextView>(R.id.tvSpeedVal)
        val tvBrightVal = findViewById<TextView>(R.id.tvBrightVal)
        val edtFrame1 = findViewById<EditText>(R.id.edtFrame1)
        val edtFrame2 = findViewById<EditText>(R.id.edtFrame2)

        switchEnable.isChecked = prefs.getBoolean("call_led_enabled", true)

        val currentSpeed = prefs.getInt("call_flash_speed", 150)
        seekSpeed.progress = currentSpeed
        tvSpeedVal.text = "Flash Speed: $currentSpeed ms"

        val currentBright = prefs.getInt("call_brightness", 255)
        seekBrightness.progress = currentBright
        tvBrightVal.text = "Brightness: $currentBright"

        edtFrame1.setText(prefs.getString("call_frame_1", "FRAME 0xFF0000 0xFF0000 0x000000 0x000000"))
        edtFrame2.setText(prefs.getString("call_frame_2", "FRAME 0x000000 0x000000 0x0000FF 0x0000FF"))

        btnDemoCall.setOnClickListener {
            CallLedAnimator.start(this)

            android.os.Handler(mainLooper).postDelayed({
                CallLedAnimator.stop(this)
            }, 3000)
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
                tvBrightVal.text = "Brightness: $progress"
                prefs.edit().putInt("call_brightness", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                prefs.edit()
                    .putString("call_frame_1", edtFrame1.text.toString())
                    .putString("call_frame_2", edtFrame2.text.toString())
                    .apply()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        edtFrame1.addTextChangedListener(textWatcher)
        edtFrame2.addTextChangedListener(textWatcher)
    }
}