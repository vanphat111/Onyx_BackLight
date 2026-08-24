package vn.vanphat111.onyxbacklight

import android.util.Log
import kotlin.concurrent.thread

object CameraState {
    var isCameraActive: Boolean = false
}

class LedController {
    private val SOCKET_NAME = "/dev/socket/onyx_led.sock"
    private val TAG = "OnyxLED"

    companion object {
        val suLock = Any()
    }

    fun sendCommand(command: String, onLog: ((String) -> Unit)? = null) {
        if (CameraState.isCameraActive && command != "OFF") {
            val blockedMsg = "BLOCKED: Camera is in use. Ignored -> $command"
            Log.w(TAG, blockedMsg)
            onLog?.invoke(blockedMsg)
            return
        }

        thread {
            synchronized(suLock) {
                try {
                    val shellCommand = "echo \"$command\" | nc -U $SOCKET_NAME"
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", shellCommand))
                    process.waitFor()

                    val successMsg = "[OK] Sent: $command"
                    Log.d(TAG, successMsg)
                    onLog?.invoke(successMsg)
                } catch (e: Exception) {
                    val errorMsg = "[FAIL] Error: ${e.message}"
                    Log.e(TAG, errorMsg)
                    onLog?.invoke(errorMsg)
                }
            }
        }
    }
}