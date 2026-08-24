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
                        when (intent?.action) {
                            Intent.ACTION_POWER_CONNECTED -> {
                                Log.d(TAG, "Charger PLUGGED IN")
                                led.sendCommand("FRAME 0x00FF00 0x00FF00 0x00FF00 0x00FF00")
                            }
                            Intent.ACTION_POWER_DISCONNECTED -> {
                                Log.d(TAG, "Charger UNPLUGGED")
                                led.sendCommand("OFF")
                            }
                        }
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
                        CameraState.isCameraActive = true
                        led.sendCommand("OFF")
                    }

                    override fun onCameraAvailable(cameraId: String) {
                        Log.d(TAG, "Camera $cameraId is CLOSED. Unblocking LED.")
                        CameraState.isCameraActive = false
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

        if (packageName.contains("zalo") || packageName.contains("orca")) {
            Log.d(TAG, "Target Notification Received from: $packageName")
            thread {
                for (i in 1..3) {
                    led.sendCommand("FRAME 0x00FFFF 0x00FFFF 0x00FFFF 0x00FFFF")
                    Thread.sleep(400)
                    led.sendCommand("OFF")
                    Thread.sleep(400)
                }
            }
        }
    }
}