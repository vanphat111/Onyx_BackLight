package vn.vanphat111.onyxbacklight

import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import java.io.InputStream
import java.io.OutputStream

object CameraState {
    var isCameraActive: Boolean = false
}

class LedController {
    private val SOCKET_NAME = "onyx_led_abstract"
    private val TAG = "OnyxLED_HAL"

    companion object {
        val suLock = Any()
    }

    fun resetForStatic() {
        sendCommand("RUN 0")
        sendCommand("EFFECT 0")
        sendCommand("TRIGGER none")
    }

    fun resetAndOff() {
        resetForStatic()
        sendCommand("OFF")
    }

    fun sendCommand(cmd: String): Boolean {
        val targetCmd = if (LedStateManager.isCameraBlocking) {
            val allowedCommands = listOf("OFF", "RUN 0", "EFFECT 0", "TRIGGER none")
            if (cmd !in allowedCommands) {
                Log.w(TAG, "Blocked command '$cmd' due to Camera Active -> Force OFF")
                "OFF"
            } else cmd
        } else {
            cmd
        }

        var socket: LocalSocket? = null
        var out: OutputStream? = null
        var input: InputStream? = null
        try {
            socket = LocalSocket()
            socket.connect(
                LocalSocketAddress(
                    SOCKET_NAME,
                    LocalSocketAddress.Namespace.ABSTRACT
                )
            )
            socket.soTimeout = 200

            out = socket.outputStream
            input = socket.inputStream

            out.write("$targetCmd\n".toByteArray())
            out.flush()

            val buffer = ByteArray(16)
            val bytesRead = input.read(buffer)
            if (bytesRead > 0) {
                val response = String(buffer, 0, bytesRead).trim()
                if (response == "OK") {
                    Log.v(TAG, "CMD '$targetCmd' -> OK")
                    return true
                } else {
                    Log.w(TAG, "CMD '$targetCmd' -> Unexpected response: '$response'")
                }
            } else {
                Log.w(TAG, "CMD '$targetCmd' -> Empty response from HAL socket")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Socket Error on CMD '$targetCmd': ${e.message}")
        } finally {
            try { input?.close() } catch (_: Exception) {}
            try { out?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
        return false
    }
}