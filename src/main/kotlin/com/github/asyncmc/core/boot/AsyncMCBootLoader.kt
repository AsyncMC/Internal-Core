@file:JvmName("AsyncMCBootLoader")

package com.github.asyncmc.core.boot

import com.github.ajalt.clikt.core.CliktCommand

fun main(args: Array<String>) = object : CliktCommand() {
    override fun run() {
        throw NotImplementedError("This is not implemented yet, sorry")
    }
}.main(args)
