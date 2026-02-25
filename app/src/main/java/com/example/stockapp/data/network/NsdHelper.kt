package com.example.stockapp.data.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class NsdHelper(private val context: Context) {

    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val SERVICE_TYPE = "_stockapp._tcp."
    private var serviceName: String? = null

    fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = "StockApp"
            this.serviceType = SERVICE_TYPE
            this.port = port
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                serviceName = nsdServiceInfo.serviceName
                Log.d("NsdHelper", "Service registered: $serviceName")
            }

            override fun onRegistrationFailed(nsdServiceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NsdHelper", "Service registration failed: $errorCode")
            }

            override fun onServiceUnregistered(nsdServiceInfo: NsdServiceInfo) {
                Log.d("NsdHelper", "Service unregistered: ${nsdServiceInfo.serviceName}")
            }

            override fun onUnregistrationFailed(nsdServiceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NsdHelper", "Service unregistration failed: $errorCode")
            }
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun discoverServices() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d("NsdHelper", "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d("NsdHelper", "Service found: $service")
                when {
                    service.serviceType != SERVICE_TYPE -> {
                        Log.d("NsdHelper", "Unknown Service Type: ${service.serviceType}")
                    }
                    service.serviceName == serviceName -> {
                        Log.d("NsdHelper", "Same machine: $serviceName")
                    }
                    service.serviceName.contains("StockApp") -> {
                        nsdManager.resolveService(service, createResolveListener())
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.e("NsdHelper", "Service lost: $service")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i("NsdHelper", "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NsdHelper", "Discovery failed: Error code: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NsdHelper", "Discovery failed: Error code: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun createResolveListener(): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NsdHelper", "Resolve failed: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d("NsdHelper", "Resolve Succeeded. $serviceInfo")
                // TODO: Connect to the service
            }
        }
    }

    fun stopDiscovery() {
        if (discoveryListener != null) {
            nsdManager.stopServiceDiscovery(discoveryListener)
            discoveryListener = null
        }
    }

    fun unregisterService() {
        if (registrationListener != null) {
            nsdManager.unregisterService(registrationListener)
            registrationListener = null
        }
    }
}
