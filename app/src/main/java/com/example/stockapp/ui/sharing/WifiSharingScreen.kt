package com.example.stockapp.ui.sharing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.stockapp.data.network.NsdHelper

@Composable
fun WifiSharingScreen() {
    val context = LocalContext.current
    val nsdHelper = remember { NsdHelper(context) }

    DisposableEffect(Unit) {
        onDispose {
            nsdHelper.stopDiscovery()
            nsdHelper.unregisterService()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = { nsdHelper.discoverServices() }) {
            Text("Discover Devices")
        }
        // TODO: Display discovered devices
    }
}
