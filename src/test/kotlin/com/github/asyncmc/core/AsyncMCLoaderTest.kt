package com.github.asyncmc.core

import com.github.asyncmc.core.boot.main
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AsyncMCLoaderTest {
    @Test
    fun testMain() {
        assertThrows<NotImplementedError> { main(emptyArray()) }
    }
}
