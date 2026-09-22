package com.example.autobook.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.autobook.data.AppDatabase
import com.example.autobook.data.Transaction
import com.example.autobook.parser.PayParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val pending = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                messages.groupBy { it.originatingAddress ?: "未知" }.forEach { (address, parts) ->
                    val body = parts.joinToString("") { it.messageBody ?: "" }
                    val parsed = PayParser.parse(body) ?: return@forEach
                    if (Dedup.isDuplicate("sms|${body.hashCode()}")) return@forEach

                    AppDatabase.get(appContext).transactionDao().insert(
                        Transaction(
                            amount = parsed.amount,
                            type = if (parsed.isIncome) 1 else 0,
                            category = parsed.category,
                            merchant = parsed.merchant.ifBlank { address },
                            note = body,
                            source = "短信 $address",
                            timestamp = System.currentTimeMillis(),
                            auto = true
                        )
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }
}
