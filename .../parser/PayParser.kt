package com.example.autobook.parser

object PayParser {

    data class Result(
        val amount: Double,
        val isIncome: Boolean,
        val category: String,
        val merchant: String
    )

    private val amountPatterns = listOf(
        Regex("""[¥￥]\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)"""),
        Regex("""(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)\s*元"""),
        Regex("""(?:金额|人民币|RMB)\s*[:：]?\s*(\d+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    )

    private val incomePattern = Regex(
        "退款|退回|返现|返还|收款|到账|入账|转入|收入|工资|报销|已领取"
    )

    private val expensePattern = Regex(
        "支出|付款|消费|支付|扣款|转出|已付|代扣|花费|缴费|还款|转账"
    )

    private val blacklist = listOf(
        "验证码", "校验码", "动态码", "优惠券", "红包已过期", "余额不足",
        "还款提醒", "账单提醒", "积分", "活动", "广告", "下载", "升级", "退订"
    )

    private val merchantPatterns = listOf(
        Regex("""向\s*(.{1,16}?)\s*(?:付款|转账|支付)"""),
        Regex("""(?:收款方|对方|商户|付款给|收款人)\s*[:：]?\s*(.{1,16}?)(?:\s|$)"""),
        Regex("""来自\s*(.{1,16}?)\s*的?(?:收款|转账|付款)"""),
        Regex("""在\s*(.{1,16}?)\s*(?:消费|支付|付款)"""),
        Regex("""【(.{1,16}?)】""")
    )

    fun parse(raw: String): Result? {
        val text = raw.replace('\n', ' ').trim()
        if (text.length < 4) return null
        if (blacklist.any { text.contains(it) }) return null

        val amount = extractAmount(text) ?: return null
        if (amount <= 0.0 || amount > 10_000_000) return null

        val isIncome = detectDirection(text) ?: return null

        val merchant = merchantPatterns
            .firstNotNullOfOrNull { p ->
                p.find(text)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
            } ?: ""

        return Result(amount, isIncome, guessCategory(text, isIncome), merchant)
    }

    private fun extractAmount(text: String): Double? {
        for (p in amountPatterns) {
            val m = p.find(text) ?: continue
            val v = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: continue
            return v
        }
        return null
    }

    private fun detectDirection(text: String): Boolean? {
        val inc = incomePattern.find(text)
        val exp = expensePattern.find(text)
        return when {
            inc == null && exp == null -> null
            inc == null -> false
            exp == null -> true
            inc.range.first <= exp.range.first -> true
            else -> false
        }
    }

    private fun guessCategory(text: String, isIncome: Boolean): String {
        if (isIncome) {
            return when {
                text.contains("退款") || text.contains("退回") -> "退款"
                text.contains("工资") -> "工资"
                text.contains("报销") -> "报销"
                text.contains("红包") -> "红包"
                else -> "收入"
            }
        }
        return when {
            listOf("餐", "饭", "外卖", "饿了么", "美团").any(text::contains) -> "餐饮"
            listOf("超市", "商场", "购物", "淘宝", "京东", "拼多多").any(text::contains) -> "购物"
            listOf("打车", "滴滴", "地铁", "公交", "加油", "高速").any(text::contains) -> "交通"
            listOf("话费", "电费", "水费", "燃气", "缴费").any(text::contains) -> "生活缴费"
            listOf("转账", "红包").any(text::contains) -> "转账"
            else -> "其他"
        }
    }
}
