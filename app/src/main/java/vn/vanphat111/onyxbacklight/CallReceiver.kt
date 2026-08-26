package vn.vanphat111.onyxbacklight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    Log.d("OnyxLED_Call", "Incoming call ring ring!")
                    CallLedAnimator.start(context)
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK,
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    Log.d("OnyxLED_Call", "Incoming call ended.")
                    CallLedAnimator.stop(context)
                }
            }
        }
    }
}