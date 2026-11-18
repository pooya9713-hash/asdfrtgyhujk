package com.homecontrol.app.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import com.homecontrol.app.HomeControlApp
import com.homecontrol.app.data.DeliveryStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Receives "sent" and "delivery report" callbacks from the system SMS service. */
class SmsStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_HISTORY_ID, -1L)
        if (id < 0) return
        val repository = (context.applicationContext as HomeControlApp).container.repository
        val code = resultCode
        val action = intent.action
        val pdu = intent.getByteArrayExtra("pdu")
        val format = intent.getStringExtra("format")
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_SENT -> {
                        if (code == Activity.RESULT_OK) {
                            repository.markSendResult(id, true, null)
                        } else {
                            repository.markSendResult(id, false, describeError(code))
                        }
                    }
                    ACTION_DELIVERED -> {
                        // TP-Status: 0 = delivered, 0x20..0x3F = still trying, >= 0x40 = permanent failure.
                        val status = runCatching { SmsMessage.createFromPdu(pdu, format).status }.getOrNull()
                        when {
                            status == 0 -> repository.markDelivery(id, DeliveryStatus.DELIVERED)
                            status != null && status >= 0x40 ->
                                repository.markDelivery(id, DeliveryStatus.DELIVERY_FAILED)
                            else -> Unit // no usable report: show nothing
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun describeError(code: Int): String = when (code) {
        1 -> "خطای عمومی در ارسال پیامک"
        2 -> "رادیو خاموش است (حالت پرواز؟)"
        3 -> "قالب پیامک نامعتبر است"
        4 -> "سرویس شبکه در دسترس نیست (آنتن‌دهی)"
        5 -> "محدودیت تعداد پیامک"
        6 -> "ارسال به این شماره مجاز نیست (FDN)"
        7, 8 -> "ارسال به این شماره توسط سیستم مسدود شد"
        else -> "ارسال ناموفق (کد $code)"
    }

    companion object {
        const val ACTION_SENT = "com.homecontrol.app.SMS_SENT"
        const val ACTION_DELIVERED = "com.homecontrol.app.SMS_DELIVERED"
        const val EXTRA_HISTORY_ID = "history_id"
    }
}
