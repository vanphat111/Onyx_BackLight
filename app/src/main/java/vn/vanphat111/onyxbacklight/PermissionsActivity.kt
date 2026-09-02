package vn.vanphat111.onyxbacklight

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.button.MaterialButton

class PermissionsActivity : AppCompatActivity() {

    private val TAG = "OnyxLED_Perms"
    private lateinit var btnNotif: MaterialButton
    private lateinit var btnBattery: MaterialButton
    private lateinit var btnAutoStart: MaterialButton
    private lateinit var btnContinue: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)
        initViews()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Start enforcing sequence...")
        enforcePermissionSequence()
    }

    private fun initViews() {
        btnNotif = findViewById(R.id.btnReqNotif)
        btnBattery = findViewById(R.id.btnReqBattery)
        btnAutoStart = findViewById(R.id.btnReqAutoStart)
        btnContinue = findViewById(R.id.btnCheckAndContinue)
    }

    private fun setupButtons() {
        btnNotif.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnBattery.setOnClickListener {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }

        btnAutoStart.setOnClickListener {
            try {
                val intent = Intent()
                intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
        }

        btnContinue.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun enforcePermissionSequence() {
        val notifOk = hasNotificationAccess()
        val batteryOk = hasBatteryIgnored()

        Log.d(TAG, "Permissions status - Notif: $notifOk, Battery: $batteryOk")

        setPermissionStyle(btnNotif, notifOk, true)

        val batteryAvailable = notifOk
        setPermissionStyle(btnBattery, batteryOk, batteryAvailable)

        val autoStartAvailable = notifOk && batteryOk
        setPermissionStyle(btnAutoStart, false, autoStartAvailable)

        btnContinue.isEnabled = autoStartAvailable
        btnContinue.alpha = if (autoStartAvailable) 1f else 0.42f

        if (autoStartAvailable) {
            Log.d(TAG, "Required permissions granted; opening MainActivity")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setPermissionStyle(
        button: MaterialButton,
        completed: Boolean,
        available: Boolean
    ) {
        val background = when {
            completed -> "#254E4C" // soft green: completed
            available -> "#202B47" // blue glass card: ready
            else -> "#151C2E"      // dark muted: locked
        }

        val stroke = when {
            completed -> "#70E1D0"
            available -> "#526487"
            else -> "#303B55"
        }

        button.isEnabled = available || completed
        button.alpha = if (available || completed) 1f else 0.45f
        button.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(background)
            )
        button.strokeColor =
            android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(stroke)
            )
    }

    private fun hasNotificationAccess(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
    }

    private fun hasBatteryIgnored(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }
}
