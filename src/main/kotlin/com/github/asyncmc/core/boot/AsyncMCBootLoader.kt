@file:JvmName("AsyncMCBootLoader")

package com.github.asyncmc.core.boot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.asyncmc.core.server.CoreServer
import kotlinx.coroutines.runBlocking

public fun main(args: Array<String>): Unit = object : CliktCommand() {
    override fun run() = runBlocking {
        val core = CoreServer(this)
        core.start()
    }
}.main(args)
