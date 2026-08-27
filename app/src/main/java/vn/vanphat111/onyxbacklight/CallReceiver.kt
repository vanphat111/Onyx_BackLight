package vn.vanphat111.onyxbacklight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {
    private val TAG = "OnyxLED_CallReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            Log.d(TAG, "Telephony State Changed: $state")

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    Log.d(TAG, "Cellular incoming call ringing -> Triggering Animator")
                    CallLedAnimator.start(context)
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK,
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    Log.d(TAG, "Cellular call ended/answered -> Stopping Animator")
                    CallLedAnimator.stop(context)
                }
            }
        }
    }
}