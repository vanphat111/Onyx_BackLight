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
    private val led = LedController()
    private var powerReceiver: BroadcastReceiver? = null
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraCallback: CameraManager.AvailabilityCallback

    override fun onCreate() {
        super.onCreate()

        powerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        led.sendCommand("FRAME 0x00FF00 0x00FF00 0x00FF00 0x00FF00")
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
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

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraCallback = object : CameraManager.AvailabilityCallback() {
            override fun onCameraUnavailable(cameraId: String) {
                // Triggered when the camera is OPENED by any app
                Log.d("OnyxLED", "Camera $cameraId is OPENED")
                CameraState.isCameraActive = true

                led.sendCommand("OFF")
            }

            override fun onCameraAvailable(cameraId: String) {
                // Triggered when the camera is CLOSED
                Log.d("OnyxLED", "Camera $cameraId is CLOSED")
                CameraState.isCameraActive = false
            }
        }
        cameraManager.registerAvailabilityCallback(cameraCallback, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        powerReceiver?.let { unregisterReceiver(it) }
        cameraManager.unregisterAvailabilityCallback(cameraCallback)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val packageName = sbn?.packageName ?: return

        if (packageName.contains("zalo") || packageName.contains("orca")) {
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