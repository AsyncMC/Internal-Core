@file:JvmName("AsyncMCBootLoader")

package com.github.asyncmc.core.boot

import com.github.ajalt.clikt.core.CliktCommand

public fun main(args: Array<String>): Unit = object : CliktCommand() {
    override fun run() {
        throw NotImplementedError("This is not implemented yet, sorry")
    }
}.main(args)
