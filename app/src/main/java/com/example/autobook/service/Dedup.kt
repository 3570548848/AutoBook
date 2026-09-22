package com.example.autobook.service

internal object Dedup {
    private const val WINDOW = 5 * 60 * 1000L
    private val seen = LinkedHashMap<String, Long>()

    @Synchronized
    fun isDuplicate(key: String): Boolean {
        val now = System.currentTimeMillis()
        val it = seen.entries.iterator()
        while (it.hasNext()) if (now - it.next().value > WINDOW) it.remove()

        if (seen.containsKey(key)) return true
        seen[key] = now
        return false
    }
}
