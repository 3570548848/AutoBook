package com.example.autobook.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.autobook.data.AppDatabase
import com.example.autobook.data.Transaction
import com.example.autobook.parser.PayParser
import kotlinx.coroutines.*

class PayNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return
        val sourceName = SOURCE_APPS[pkg] ?: return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val content = listOf(title, text, big)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        if (content.isBlank()) return

        val parsed = PayParser.parse(content) ?: return

        val key = "$pkg|${content.hashCode()}|${System.currentTimeMillis() / 60_000}"
        if (Dedup.isDuplicate(key)) return

        scope.launch {
            runCatching {
                AppDatabase.get(this@PayNotificationListener).transactionDao().insert(
                    Transaction(
                        amount = parsed.amount,
                        type = if (parsed.isIncome) 1 else 0,
                        category = parsed.category,
                        merchant = parsed.merchant.ifBlank { sourceName },
                        note = content,
                        source = sourceName,
                        timestamp = sbn.postTime,
                        auto = true
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private val SOURCE_APPS = mapOf(
            "com.tencent.mm" to "微信",
            "com.eg.android.AlipayGphone" to "支付宝",
            "com.unionpay" to "云闪付",
            "com.icbc" to "工商银行",
            "cmb.pb" to "招商银行",
            "com.chinamworld.main" to "建设银行"
        )
    }
}
