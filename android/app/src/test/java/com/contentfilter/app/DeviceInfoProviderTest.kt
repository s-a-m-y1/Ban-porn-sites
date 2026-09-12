package com.contentfilter.app

import org.junit.Test
import org.junit.Assert.*

class DeviceInfoProviderTest {
    @Test fun deviceInfoDefaults() {
        val info = DeviceInfo(manufacturer = "Google", model = "Pixel 7", androidVersion = "14", apiLevel = 34, serial = "ABC123", resolution = "1080x2400", batteryLevel = 85)
        assertEquals("Google Pixel 7", info.displayName)
        assertEquals(85, info.batteryLevel)
        assertTrue(info.isCharging)
    }
    @Test fun displayNameFallback() {
        val info = DeviceInfo(manufacturer = "", model = "", serial = "XYZ")
        assertEquals("XYZ", info.displayName)
    }
}
