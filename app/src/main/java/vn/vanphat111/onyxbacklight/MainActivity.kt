package vn.vanphat111.onyxbacklight

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val TAG = "OnyxLED_Main"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupFeatureCards()
    }

    private fun setupFeatureCards() {
        val cardNotif = findViewById<LinearLayout>(R.id.cardNotifFeature)
        val switchNotif = findViewById<Switch>(R.id.switchNotif)

        cardNotif.setOnClickListener {
            val intent = android.content.Intent(this, NotificationSettingsActivity::class.java)
            startActivity(intent)
        }

        val prefs = getSharedPreferences("OnyxPrefs", MODE_PRIVATE)
        switchNotif.isChecked = prefs.getBoolean("notif_enabled", false)

        switchNotif.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_enabled", isChecked).apply()
            Log.d(TAG, "Notification LED feature status: $isChecked")

            LedStateManager.restoreBaseState(applicationContext)
        }


        val cardCharging = findViewById<LinearLayout>(R.id.cardChargingFeature)
        val switchCharging = findViewById<Switch>(R.id.switchCharging)

        switchCharging.isChecked = prefs.getBoolean("charging_enabled", false)

        cardCharging.setOnClickListener {
            val intent = android.content.Intent(this, ChargingSettingsActivity::class.java)
            startActivity(intent)
        }

        switchCharging.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("charging_enabled", isChecked).apply()
            Log.d(TAG, "Charging Status feature: $isChecked")

            LedStateManager.restoreBaseState(applicationContext)
        }
    }
}