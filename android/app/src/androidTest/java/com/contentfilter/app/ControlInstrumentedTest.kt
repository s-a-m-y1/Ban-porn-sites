package com.contentfilter.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ControlInstrumentedTest {
    @Test fun deviceInfoProvider() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val info = DeviceInfoProvider.get(ctx)
        assertTrue(info.manufacturer.isNotEmpty())
        assertTrue(info.model.isNotEmpty())
        assertTrue(info.androidVersion.isNotEmpty())
        assertTrue(info.apiLevel >= 24)
        assertTrue(info.resolution.contains("x"))
    }
    @Test fun controlServiceLifecycle() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Just verify provider doesn't crash and service constants
        assertEquals("control_service_channel", ControlService.CHANNEL_ID)
    }
}
