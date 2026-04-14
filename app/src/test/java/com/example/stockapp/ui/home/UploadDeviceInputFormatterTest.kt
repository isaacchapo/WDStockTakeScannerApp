package com.example.stockapp.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class UploadDeviceInputFormatterTest {

    @Test
    fun sanitizeDeviceBaseUrlInput_trimsInputAndTrailingSlash() {
        val sanitized = sanitizeDeviceBaseUrlInput("  https://192.168.1.10:8080/  ")

        assertEquals("https://192.168.1.10:8080", sanitized)
    }

    @Test
    fun buildDeviceBaseUrl_prependsHttpScheme() {
        val url = buildDeviceBaseUrl("192.168.1.10:8080")

        assertEquals("http://192.168.1.10:8080", url)
    }

    @Test
    fun buildDeviceBaseUrl_preservesHttpsScheme() {
        val url = buildDeviceBaseUrl(" https://192.168.1.10:8080/ ")

        assertEquals("https://192.168.1.10:8080", url)
    }

    @Test
    fun sanitizeEndpointSuffixInput_handlesManualSuffix() {
        val suffix = sanitizeEndpointSuffixInput(" /v2/items ")

        assertEquals("v2/items", suffix)
    }

    @Test
    fun sanitizeEndpointSuffixInput_keepsTypedPathContent() {
        val suffix = sanitizeEndpointSuffixInput("api/stock/upload/v2/items")

        assertEquals("api/stock/upload/v2/items", suffix)
    }

    @Test
    fun sanitizeEndpointSuffixInput_stripsHostFromFullUrl() {
        val suffix = sanitizeEndpointSuffixInput("http://192.168.1.7:5000/api/stock/upload/v2")

        assertEquals("api/stock/upload/v2", suffix)
    }

    @Test
    fun buildEndpointPath_defaultsToPpWhenSuffixIsEmpty() {
        val endpointPath = buildEndpointPath("")

        assertEquals("/p/p", endpointPath)
    }

    @Test
    fun buildEndpointPath_addsLeadingSlashToTypedPath() {
        val endpointPath = buildEndpointPath("api/stock/upload/v2")

        assertEquals("/api/stock/upload/v2", endpointPath)
    }
}
