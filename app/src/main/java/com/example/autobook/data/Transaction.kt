package com.example.autobook.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "txn")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amount: Double,
    val type: Int,
    val category: String,
    val merchant: String,
    val note: String,
    val source: String,
    val timestamp: Long,
    val auto: Boolean
) {
    val isIncome: Boolean get() = type == 1
}
