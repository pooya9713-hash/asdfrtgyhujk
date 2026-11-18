package com.homecontrol.app.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

class SmsSender(private val context: Context) {

    /** True when the device has telephony and the SIM used for SMS is ready. */
    fun isSimReady(): Boolean {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) return false
        val tm = context.getSystemService(TelephonyManager::class.java) ?: return false
        val subId = SmsManager.getDefaultSmsSubscriptionId()
        val target = if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            tm.createForSubscriptionId(subId)
        } else {
            tm
        }
        return target.simState == TelephonyManager.SIM_STATE_READY
    }

    /**
     * Hands the message to the system SMS service. Success/failure and delivery are reported
     * asynchronously to [SmsStatusReceiver] together with [historyId].
     * Throws SecurityException if SEND_SMS is not granted, or other exceptions on failure.
     */
    fun send(number: String, text: String, historyId: Long) {
        val manager: SmsManager = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        manager.sendTextMessage(
            number,
            null,
            text,
            statusIntent(SmsStatusReceiver.ACTION_SENT, historyId),
            statusIntent(SmsStatusReceiver.ACTION_DELIVERED, historyId)
        )
    }

    private fun statusIntent(action: String, historyId: Long): PendingIntent {
        val intent = Intent(action)
            .setClass(context, SmsStatusReceiver::class.java)
            .putExtra(SmsStatusReceiver.EXTRA_HISTORY_ID, historyId)
        val requestCode = (historyId * 2 + if (action == SmsStatusReceiver.ACTION_SENT) 0 else 1).toInt()
        // The system fills in result extras (e.g. the delivery report PDU), so the intent must be mutable.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
