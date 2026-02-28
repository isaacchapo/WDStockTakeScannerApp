package com.example.stockapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.stockapp.data.StockRepository

class CreateStockViewModelFactory(
    private val repository: StockRepository,
    private val ownerUid: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateStockViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateStockViewModel(repository, ownerUid) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
