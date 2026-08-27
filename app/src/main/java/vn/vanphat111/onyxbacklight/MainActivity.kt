package vn.vanphat111.onyxbacklight

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.content.Intent

class MainActivity : AppCompatActivity() {
    private val TAG = "OnyxLED_Main"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting READ_PHONE_STATE permission")
            requestPermissions(arrayOf(android.Manifest.permission.READ_PHONE_STATE), 101)
        }

        setupFeatureCards()
        val btnRgbWave = findViewById<Button>(R.id.btnRgbWaveToggle)

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

        findViewById<Button>(R.id.btnCallSettings).setOnClickListener {
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

    private fun updateWaveButtonState(button: Button) {
        if (ColorWaveAnimator.isWaveActive()) {
            button.text = "Disable RGB wave"
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#B31312")))
        } else {
            button.text = "🌀 Enable RGB wave"
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2D3250")))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            ColorWaveAnimator.stop(applicationContext)
        }
    }
}