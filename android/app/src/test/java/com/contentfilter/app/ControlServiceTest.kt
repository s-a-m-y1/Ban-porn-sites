package com.contentfilter.app

import org.junit.Test
import org.junit.Assert.*

class ControlServiceTest {
    @Test fun constants() {
        assertEquals("control_service_channel", ControlService.CHANNEL_ID)
        assertEquals(2002, ControlService.NOTIF_ID)
    }
}
