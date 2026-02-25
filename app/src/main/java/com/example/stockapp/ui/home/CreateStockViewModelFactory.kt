package com.example.stockapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.stockapp.data.StockRepository

class CreateStockViewModelFactory(private val repository: StockRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateStockViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateStockViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}