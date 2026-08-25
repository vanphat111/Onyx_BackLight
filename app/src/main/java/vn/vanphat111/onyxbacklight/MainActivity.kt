package vn.vanphat111.onyxbacklight

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
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
        }

        val cardCharging = findViewById<LinearLayout>(R.id.cardChargingFeature)
        val switchCharging = findViewById<Switch>(R.id.switchCharging)

        cardCharging.setOnClickListener {
        }

        switchCharging.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Charging Status feature status: $isChecked")
        }
    }
}