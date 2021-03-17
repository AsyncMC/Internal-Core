/*
 *  AsyncMC - A fully async, non blocking, thread safe and open source Minecraft server implementation
 *  Copyright (C) 2021  José Roberto de Araújo Júnior <joserobjr@gamemods.com.br>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published
 *  by the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.asyncmc.core.server

import com.github.asyncmc.module.api.*
import com.github.michaelbull.logging.InlineLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

/**
 * This is the deepest core of the server, the goal of this object is to manage the server modules and make them
 * talk with each other.
 *
 * @author joserobjr
 */
internal class CoreServer (
    scope: CoroutineScope,
    moduleLoaders: Iterable<ModuleLoader> = ServiceLoader.load(ModuleLoader::class.java),
): Server {
    private val log = InlineLogger()
    override val uuid = UUID.randomUUID()!!
    init { log.debug { "Creating a new CoreServer instance with hashCode: ${super.hashCode()} and UUID: $uuid" } }
    private val _lifecycle = MutableStateFlow(ServerLifecycle.CORE_INITIALIZATION)
    override val lifecycle = _lifecycle.asStateFlow()

    private val modules = with(scope) {
        moduleLoaders.flatMap { loader ->
            log.debug { "The module loader ${loader::class.java.name} was found, creating modules..." }
            with(loader) {
                create(this@CoreServer).also {
                    log.debug {
                        "The loader ${loader::class.java.name} created ${it.size} modules with these classes: ${it.map { it::class.java }}"
                    }
                }.values
            }
        }
    }

    private var moduleSearchCache = mapOf<Class<out ModuleAPI>, List<ModuleAPI>>()

    @Suppress("UNCHECKED_CAST", "SuspiciousCollectionReassignment")
    override fun <M : ModuleAPI> findModules(moduleClass: Class<M>): List<M> {
        moduleSearchCache[moduleClass]?.let { return it as List<M> }

        log.trace { "Searching for modules compatible with ${moduleClass.name}" }
        modules.filter { moduleClass.isInstance(it.module) }
            .map { moduleClass.cast(it.module) }
            .also { moduleSearchCache += moduleClass to it }
            .also { log.trace { "Found ${it.size}: $it, the result was cached. Cache size: ${moduleSearchCache.size}" } }
            .let { return it }
    }

    internal suspend fun start() = coroutineScope {
        log.info { "Welcome! Starting AsyncMc server." }
        watchLifeStateChangeAsync()
        initializeModules()
    }

    internal fun CoroutineScope.initializeModules() {
        check(lifecycle.value == ServerLifecycle.CORE_INITIALIZATION) { "Attempted to initialize modules outside the core initialization" }
        listOf(
            ServerLifecycle.MODULE_PRE_INIT to ModuleLifecycle.PRE_INIT,
            ServerLifecycle.MODULE_INIT to ModuleLifecycle.INIT,
            ServerLifecycle.MODULE_POST_INIT to ModuleLifecycle.POST_INIT,
        ).forEach { (serverLifecycle, moduleLifecycle) ->
            _lifecycle.value = serverLifecycle
            modules.forEach { access ->
                val module = access.module
                log.debug { "Applying lifecycle $moduleLifecycle to the module ${module::class.java} from ${module.moduleLoader::class.java}" }
                try {
                    with(access) { changeLifecycleState(moduleLifecycle) }
                    log.trace { "The module has successfully changed to the $moduleLifecycle lifecycle (${module.name} - ${module.moduleLoader.name}" }
                } catch (e: Throwable) {
                    log.error(e) {
                        try {
                            "Failed to change a module to the lifecycle $moduleLifecycle: " +
                                    "(${module.name} - ${module.moduleLoader.name}) " +
                                    "(${module::class.java} from ${module.moduleLoader::class.java}) - $module"
                        } catch (e2: Throwable) {
                            e.addSuppressed(e2)
                            "Failed to change a module to the lifecycle $moduleLifecycle: " +
                                    "(${module::class.java} from ${module.moduleLoader::class.java})"
                        }
                    }
                }
            }
        }
    }

    private fun CoroutineScope.watchLifeStateChangeAsync() = launch {
        lifecycle.collect {
            log.info { "====== AsyncMC Server Life State changed to $it ======" }
        }
    }
}
