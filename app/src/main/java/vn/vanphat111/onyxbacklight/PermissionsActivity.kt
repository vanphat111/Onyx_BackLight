package vn.vanphat111.onyxbacklight

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.concurrent.thread

class PermissionsActivity : AppCompatActivity() {

    private val TAG = "OnyxLED_Perms"
    private lateinit var btnRoot: Button
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
        btnNotif = findViewById(R.id.btnReqNotif)
        btnBattery = findViewById(R.id.btnReqBattery)
        btnAutoStart = findViewById(R.id.btnReqAutoStart)
        btnContinue = findViewById(R.id.btnCheckAndContinue)
    }

    private fun setupButtons() {
        btnRoot.setOnClickListener {
            Toast.makeText(this, "Grant Root in KernelSU, then FORCE STOP this app and reopen!", Toast.LENGTH_LONG).show()
        }

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

        thread {
            val rootOk = hasRoot()

            runOnUiThread {
                if (rootOk) {
                    btnRoot.backgroundTintList = android.content.res.ColorStateList.valueOf(colorSuccess)
                    btnRoot.isEnabled = true

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
                } else {
                    btnRoot.backgroundTintList = android.content.res.ColorStateList.valueOf(colorPending)
                    btnRoot.isEnabled = true

                    disableButton(btnNotif, colorDisabled)
                    disableButton(btnBattery, colorDisabled)
                    disableButton(btnAutoStart, colorDisabled)
                    disableButton(btnContinue, colorDisabled)
                }
            }
        }
    }

    private fun disableButton(button: Button, color: Int) {
        button.isEnabled = false
        button.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
    }

    private fun hasRoot(): Boolean {
        synchronized(LedController.suLock) {
            return try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readLine()
                process.waitFor()
                process.destroy()

                val isRoot = output != null && output.contains("uid=0(root)")
                Log.d(TAG, "hasRoot check result: $isRoot")
                isRoot
            } catch (e: Exception) {
                Log.e(TAG, "hasRoot check failed: ${e.message}")
                false
            }
        }
    }

    private fun hasNotificationAccess(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
    }

    private fun hasBatteryIgnored(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }
}