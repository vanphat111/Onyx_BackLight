package vn.vanphat111.onyxbacklight

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

class PermissionsActivity : AppCompatActivity() {

    private val TAG = "OnyxLED_Perms"
    private var btnRoot: Button? = null
    private lateinit var btnNotif: Button
    private lateinit var btnBattery: Button
    private lateinit var btnAutoStart: Button
    private lateinit var btnContinue: Button

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
        btnRoot = findViewById(R.id.btnReqRoot)
        btnRoot?.visibility = View.GONE

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
        val colorSuccess = android.graphics.Color.parseColor("#00FF00")
        val colorPending = android.graphics.Color.parseColor("#444444")
        val colorDisabled = android.graphics.Color.parseColor("#1A1A1A")

        val notifOk = hasNotificationAccess()
        val batteryOk = hasBatteryIgnored()

        Log.d(TAG, "Permissions status - Notif: $notifOk, Battery: $batteryOk")

        btnNotif.isEnabled = true
        btnNotif.backgroundTintList = android.content.res.ColorStateList.valueOf(if (notifOk) colorSuccess else colorPending)

        if (notifOk) {
            btnBattery.isEnabled = true
            btnBattery.backgroundTintList = android.content.res.ColorStateList.valueOf(if (batteryOk) colorSuccess else colorPending)

            if (batteryOk) {
                btnAutoStart.isEnabled = true
                btnAutoStart.backgroundTintList = android.content.res.ColorStateList.valueOf(colorPending)

                btnContinue.isEnabled = true
                btnContinue.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2196F3"))

                Log.d(TAG, "Sequence COMPLETE. Launching MainActivity.")
                startActivity(Intent(this@PermissionsActivity, MainActivity::class.java))
                finish()
            } else {
                disableButton(btnAutoStart, colorDisabled)
                disableButton(btnContinue, colorDisabled)
            }
        } else {
            disableButton(btnBattery, colorDisabled)
            disableButton(btnAutoStart, colorDisabled)
            disableButton(btnContinue, colorDisabled)
        }
    }

    private fun disableButton(button: Button, color: Int) {
        button.isEnabled = false
        button.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
    }

    private fun hasNotificationAccess(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
    }

    private fun hasBatteryIgnored(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }
}