package com.example.autobook.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.autobook.data.AppDatabase
import com.example.autobook.data.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).transactionDao()

    val transactions: StateFlow<List<Transaction>> =
        dao.observeAll().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    fun addManual(amount: Double, isIncome: Boolean, merchant: String, note: String) {
        viewModelScope.launch {
            dao.insert(
                Transaction(
                    amount = amount,
                    type = if (isIncome) 1 else 0,
                    category = "手动",
                    merchant = merchant.ifBlank { "手动记账" },
                    note = note,
                    source = "手动",
                    timestamp = System.currentTimeMillis(),
                    auto = false
                )
            )
        }
    }

    fun delete(t: Transaction) = viewModelScope.launch { dao.delete(t) }
}
