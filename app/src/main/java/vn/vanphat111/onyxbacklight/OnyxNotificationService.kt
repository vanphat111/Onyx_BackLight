package vn.vanphat111.onyxbacklight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlin.concurrent.thread

class OnyxNotificationService : NotificationListenerService() {
    private val TAG = "OnyxLED_Service"
    private val led = LedController()
    private var powerReceiver: BroadcastReceiver? = null
    private var cameraManager: CameraManager? = null
    private var cameraCallback: CameraManager.AvailabilityCallback? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification Listener CONNECTED safely by system_server")
        setupRadarSystems()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification Listener DISCONNECTED")
        teardownRadarSystems()
    }

    private fun setupRadarSystems() {
        try {
            if (powerReceiver == null) {
                powerReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        context?.let { LedStateManager.restoreBaseState(it) }
                    }
                }
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_POWER_CONNECTED)
                    addAction(Intent.ACTION_POWER_DISCONNECTED)
                }
                registerReceiver(powerReceiver, filter)
                Log.d(TAG, "Power Radar registered")
            }

            if (cameraManager == null) {
                cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                cameraCallback = object : CameraManager.AvailabilityCallback() {
                    override fun onCameraUnavailable(cameraId: String) {
                        Log.w(TAG, "Camera $cameraId is OPENED! Blocking LED.")
                        LedStateManager.isCameraBlocking = true
                        LedStateManager.restoreBaseState(this@OnyxNotificationService)
                    }

                    override fun onCameraAvailable(cameraId: String) {
                        Log.d(TAG, "Camera $cameraId is CLOSED. Unblocking LED.")
                        LedStateManager.isCameraBlocking = false
                        LedStateManager.restoreBaseState(this@OnyxNotificationService)
                    }
                }
                cameraManager?.registerAvailabilityCallback(cameraCallback!!, null)
                Log.d(TAG, "Camera Radar registered")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up radars: ${e.message}")
        }
    }

    private fun teardownRadarSystems() {
        try {
            powerReceiver?.let {
                unregisterReceiver(it)
                powerReceiver = null
                Log.d(TAG, "Power Radar unregistered")
            }
            cameraCallback?.let {
                cameraManager?.unregisterAvailabilityCallback(it)
                cameraCallback = null
                cameraManager = null
                Log.d(TAG, "Camera Radar unregistered")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error tearing down radars: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        teardownRadarSystems()
        Log.d(TAG, "Service Destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val packageName = sbn?.packageName ?: return

        val prefs = applicationContext.getSharedPreferences("OnyxPrefs", Context.MODE_PRIVATE)

        val isEnabled = prefs.getBoolean("notif_enabled", false)
        if (!isEnabled) return

        val allowedApps = prefs.getStringSet("allowed_apps", setOf()) ?: setOf()
        if (!allowedApps.contains(packageName)) return

        val colorCommand = prefs.getString("notif_color", "FRAME 0x00FFFF 0x00FFFF 0x00FFFF 0x00FFFF") ?: return
        val brightness = prefs.getInt("notif_brightness", 128)
        val blinkCount = prefs.getInt("notif_blink_count", 3)
        val blinkSpeed = prefs.getInt("notif_blink_speed", 400).toLong()

        Log.d(TAG, "Target Notification Received from: $packageName | Blinks: $blinkCount | Speed: ${blinkSpeed}ms")

        thread {
            led.sendCommand("BRIGHTNESS $brightness")
            for (i in 1..blinkCount) {
                led.sendCommand(colorCommand)
                Thread.sleep(blinkSpeed)
                led.sendCommand("OFF")
                Thread.sleep(blinkSpeed)
            }
            LedStateManager.restoreBaseState(applicationContext)
        }
    }
}