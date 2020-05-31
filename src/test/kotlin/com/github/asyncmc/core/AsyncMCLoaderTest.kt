package com.github.asyncmc.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AsyncMCLoaderTest {
    @Test
    fun testMain() {
        assertThrows<NotImplementedError> { main(emptyArray()) }
    }
}
