package com.maidcleaner

import com.maidcleaner.util.SizeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class SizeFormatterTest {

    @Test
    fun formatZeroBytes() {
        assertEquals("0 B", SizeFormatter.format(0))
    }

    @Test
    fun formatBytes() {
        assertEquals("512 B", SizeFormatter.format(512))
    }

    @Test
    fun formatKilobytes() {
        val result = SizeFormatter.format(1536)
        assert(result.contains("KB"))
    }

    @Test
    fun formatMegabytes() {
        val result = SizeFormatter.format(5 * 1024 * 1024 + 512 * 1024)
        assert(result.contains("MB"))
    }

    @Test
    fun formatGigabytes() {
        val result = SizeFormatter.format(2L * 1024 * 1024 * 1024)
        assert(result.contains("GB"))
    }

    @Test
    fun formatNegativeBytes() {
        assertEquals("0 B", SizeFormatter.format(-1))
    }

    @Test
    fun formatLargeTerabytes() {
        val result = SizeFormatter.format(3L * 1024 * 1024 * 1024 * 1024)
        assert(result.contains("TB"))
    }
}
