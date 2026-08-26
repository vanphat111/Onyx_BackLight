package vn.vanphat111.onyxbacklight

import android.net.LocalSocket
import android.net.LocalSocketAddress
import java.io.InputStream
import java.io.OutputStream

object CameraState {
    var isCameraActive: Boolean = false
}

class LedController {
    private val SOCKET_NAME = "onyx_led_abstract"
    private val TAG = "OnyxLED"

    companion object {
        val suLock = Any()
    }

    fun sendCommand(cmd: String): Boolean {
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

            out.write("$cmd\n".toByteArray())
            out.flush()

            val buffer = ByteArray(16)
            val bytesRead = input.read(buffer)
            if (bytesRead > 0) {
                val response = String(buffer, 0, bytesRead).trim()
                if (response == "OK") {
                    return true
                }
            }
        } catch (e: Exception) {
             e.printStackTrace()
        } finally {
            input?.close()
            out?.close()
            socket?.close()
        }
        return false
    }
}