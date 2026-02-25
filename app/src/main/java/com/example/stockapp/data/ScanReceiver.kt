package com.example.stockapp.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScanReceiver(private val onScan: (String) -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.stockapp.ACTION_SCAN") {
            val scannedData = intent.getStringExtra("com.stockapp.SCAN_DATA")
            if (scannedData != null) {
                onScan(scannedData)
            }
        }
    }
}
