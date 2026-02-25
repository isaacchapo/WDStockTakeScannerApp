package com.example.stockapp

import android.app.Application
import com.example.stockapp.data.StockRepository
import com.example.stockapp.data.local.AppDatabase

class StockApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { StockRepository(database.stockItemDao(), database.userDao()) }
}
