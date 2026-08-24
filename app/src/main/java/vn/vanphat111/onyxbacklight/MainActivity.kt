package vn.vanphat111.onyxbacklight

import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val led = LedController()

        // Handle color buttons
        findViewById<Button>(R.id.btnRed).setOnClickListener {
            led.sendCommand("FRAME 0xFF0000 0xFF0000 0xFF0000 0xFF0000")
        }

        findViewById<Button>(R.id.btnGreen).setOnClickListener {
            led.sendCommand("FRAME 0x00FF00 0x00FF00 0x00FF00 0x00FF00")
        }

        findViewById<Button>(R.id.btnBlue).setOnClickListener {
            led.sendCommand("FRAME 0x0000FF 0x0000FF 0x0000FF 0x0000FF")
        }

        findViewById<Button>(R.id.btnOff).setOnClickListener {
            led.sendCommand("OFF")
        }

        // Handle brightness slider
        findViewById<SeekBar>(R.id.seekBrightness).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    led.sendCommand("BRIGHTNESS $progress")
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}