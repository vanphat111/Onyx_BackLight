package vn.vanphat111.onyxbacklight

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private val TAG = "OnyxLED_Main"
    private var waveBorderAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting READ_PHONE_STATE permission")
            requestPermissions(arrayOf(android.Manifest.permission.READ_PHONE_STATE), 101)
        }

        setupFeatureCards()
        val btnRgbWave = findViewById<MaterialButton>(R.id.btnRgbWaveToggle)

        updateWaveButtonState(btnRgbWave)

        btnRgbWave.setOnClickListener {
            if (!ColorWaveAnimator.isWaveActive()) {
                Log.d(TAG, "RGB Wave Toggle -> Start")
                ColorWaveAnimator.start(applicationContext)
            } else {
                Log.d(TAG, "RGB Wave Toggle -> Stop")
                ColorWaveAnimator.stop(applicationContext)
            }
            updateWaveButtonState(btnRgbWave)
        }

        findViewById<MaterialButton>(R.id.btnCallSettings).setOnClickListener {
            startActivity(Intent(this, CallSettingsActivity::class.java))
        }
    }

    private fun setupFeatureCards() {
        val cardNotif = findViewById<LinearLayout>(R.id.cardNotifFeature)
        val switchNotif = findViewById<Switch>(R.id.switchNotif)

        cardNotif.setOnClickListener {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
        }

        val prefs = getSharedPreferences("OnyxPrefs", MODE_PRIVATE)
        switchNotif.isChecked = prefs.getBoolean("notif_enabled", false)

        switchNotif.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_enabled", isChecked).apply()
            Log.d(TAG, "Notification LED master switch: $isChecked")
            LedStateManager.restoreBaseState(applicationContext)
        }

        val cardCharging = findViewById<LinearLayout>(R.id.cardChargingFeature)
        val switchCharging = findViewById<Switch>(R.id.switchCharging)

        switchCharging.isChecked = prefs.getBoolean("charging_enabled", false)

        cardCharging.setOnClickListener {
            startActivity(Intent(this, ChargingSettingsActivity::class.java))
        }

        switchCharging.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("charging_enabled", isChecked).apply()
            Log.d(TAG, "Charging LED master switch: $isChecked")
            LedStateManager.restoreBaseState(applicationContext)
        }
    }

    private fun updateWaveButtonState(button: MaterialButton) {
        if (ColorWaveAnimator.isWaveActive()) {
            button.text = "🌀 Disable RGB wave"
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2D3250")))
            startWaveBorderAnimation(button)
        } else {
            button.text = "🌀 Enable RGB wave"
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2D3250")))
            stopWaveBorderAnimation(button)
        }
    }

    private fun startWaveBorderAnimation(button: MaterialButton) {
        if (waveBorderAnimator?.isRunning == true) return

        waveBorderAnimator = ValueAnimator.ofArgb(
            android.graphics.Color.RED,
            android.graphics.Color.MAGENTA,
            android.graphics.Color.BLUE,
            android.graphics.Color.CYAN,
            android.graphics.Color.GREEN,
            android.graphics.Color.YELLOW,
            android.graphics.Color.RED
        ).apply {
            duration = 1800L
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                button.strokeColor = ColorStateList.valueOf(animator.animatedValue as Int)
            }
            start()
        }
    }

    private fun stopWaveBorderAnimation(button: MaterialButton) {
        waveBorderAnimator?.cancel()
        waveBorderAnimator = null
        button.strokeColor = ColorStateList.valueOf(
            android.graphics.Color.parseColor("#526487")
        )
    }

    override fun onDestroy() {
        waveBorderAnimator?.cancel()
        waveBorderAnimator = null
        super.onDestroy()
        if (isFinishing) {
            ColorWaveAnimator.stop(applicationContext)
        }
    }
}
