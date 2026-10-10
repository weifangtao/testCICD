package com.example.testdemo.lib.base.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnterKeyDebouncerTest {

    @Test
    fun `first event is accepted`() {
        val debouncer = EnterKeyDebouncer(500) { 1_000L }
        assertFalse(debouncer.shouldDebounce())
    }

    @Test
    fun `event within interval is ignored`() {
        var now = 1_000L
        val debouncer = EnterKeyDebouncer(500) { now }

        assertFalse(debouncer.shouldDebounce())

        now = 1_200L
        assertTrue(debouncer.shouldDebounce())
    }

    @Test
    fun `event right at interval boundary is accepted`() {
        var now = 1_000L
        val debouncer = EnterKeyDebouncer(500) { now }

        assertFalse(debouncer.shouldDebounce())

        now = 1_500L
        assertFalse(debouncer.shouldDebounce())
    }

    @Test
    fun `zero interval disables debounce`() {
        val debouncer = EnterKeyDebouncer(0) { 1_000L }
        assertFalse(debouncer.shouldDebounce())
        assertFalse(debouncer.shouldDebounce())
    }

    @Test
    fun `negative interval is coerced to zero`() {
        val debouncer = EnterKeyDebouncer(-100) { 1_000L }
        assertFalse(debouncer.shouldDebounce())
        assertFalse(debouncer.shouldDebounce())
    }

    @Test
    fun `interval can be updated at runtime`() {
        var now = 1_000L
        val debouncer = EnterKeyDebouncer(500) { now }

        assertFalse(debouncer.shouldDebounce())
        now = 1_100L
        assertTrue(debouncer.shouldDebounce())

        debouncer.setDebounceInterval(50)
        assertFalse(debouncer.shouldDebounce())
    }
}
