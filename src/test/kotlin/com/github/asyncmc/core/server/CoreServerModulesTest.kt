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
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mockito
import java.net.URL
import kotlin.test.assertEquals

/**
 * @author joserobjr
 * @since 2021-03-17
 */
internal class CoreServerModulesTest {
    private lateinit var scope: CoroutineScope
    private lateinit var loader1: FakeModuleLoader
    private lateinit var loader2: FakeModuleLoader

    @BeforeEach
    fun setUp() {
        scope = CoroutineScope(Dispatchers.Default)
        loader1 = mockLoader().also {
            whenever(it.name).thenReturn("loader1")
            whenever(with(any<CoroutineScope>()){with(it) {createModules(any(), any())}}).doAnswer { call ->
                val loader = call.mock as ModuleLoader
                val server = call.getArgument<Server>(1)
                val secrets = call.getArgument<ModuleLoader.LoadingSecrets>(2)
                listOf(
                    mockModule1(loader, server, "moduleA", secrets),
                    mockModule1(loader, server, "moduleB", secrets),
                    mockModule2(loader, server, "moduleC", secrets),
                )
            }
        }
        loader2 = mockLoader().also {
            whenever(it.name).thenReturn("loader2")
            whenever(with(any<CoroutineScope>()){with(it) {createModules(any(), any())}}).doAnswer { call ->
                val loader = call.mock as ModuleLoader
                val server = call.getArgument<Server>(1)
                val secrets = call.getArgument<ModuleLoader.LoadingSecrets>(2)
                listOf(
                    mockModule2(loader, server,"moduleD", secrets),
                )
            }
        }
    }

    @AfterEach
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun modules3() {
        val server = CoreServer(scope, listOf(loader1, loader2))
        assertEquals(emptyList(), server.findModules(TestService3::class.java))
        assertEquals(emptyList(), server.modules<TestService3>())
    }

    @Test
    fun modules2() {
        val server = CoreServer(scope, listOf(loader1, loader2))

        val modules = server.modules<TestService2>().map { it.module }
        assertEquals(2, modules.size)
        assertEquals(setOf("moduleC", "moduleD"), modules.map{it.name}.toSet())

        val modules2 = server.findModules(TestService2::class.java).map { it.module }
        assertEquals(modules, modules2)

        assertEquals(setOf("loader1", "loader2"), modules.map { it.moduleLoader.name }.toSet())
    }

    @Test
    fun modules1() {
        val server = CoreServer(scope, listOf(loader1, loader2))

        val modules = server.modules<TestService1>().map { it.module }
        assertEquals(2, modules.size)
        assertEquals(setOf("moduleA", "moduleB"), modules.map{it.name}.toSet())

        val modules2 = server.findModules(TestService1::class.java).map { it.module }
        assertEquals(modules, modules2)

        assertEquals(listOf("loader1", "loader1"), modules.map { it.moduleLoader.name })
    }

    @Test
    //@Timeout(5)
    fun testInit() = runBlocking {
        val server = CoreServer(scope, listOf(loader1, loader2))

        val modules = server.modules<ModuleAPI>()
        assertEquals(4, modules.size)
        assertEquals(setOf("moduleA","moduleB","moduleC","moduleD"), modules.map { it.module.name }.toSet())

        val verifications = modules.map { it.module }.map { module ->
            assertEquals(ModuleLifecycle.CONSTRUCTION, module.lifecycle.value)
            async {
                var expected = ModuleLifecycle.CONSTRUCTION
                assertEquals(expected, module.lifecycle.value)
                expected = ModuleLifecycle.PRE_INIT
                module.lifecycle.collect {
                    assertEquals(expected, it)
                    when (val next = it.next) {
                        null -> cancel()
                        else -> expected = next
                    }
                }
                assertEquals(ModuleLifecycle.INITIALIZED, module.lifecycle.value)
            }
        }

        with(server) { initializeModules() }

        verifications.awaitAll()
        modules.forEach { assertEquals(ModuleLifecycle.INITIALIZED, it.module.lifecycle.value) }
    }

    private fun mockLoader() = mock<FakeModuleLoader>(defaultAnswer = Answers.CALLS_REAL_METHODS)
    private fun mockModule1(loader: ModuleLoader, server: Server, name: String, secrets: ModuleLoader.LoadingSecrets) =
        Mockito.mock(FakeModule1::class.java, Mockito.withSettings().useConstructor(loader, server, name, secrets).defaultAnswer(Answers.CALLS_REAL_METHODS))
    private fun mockModule2(loader: ModuleLoader, server: Server, name: String, secrets: ModuleLoader.LoadingSecrets) =
        Mockito.mock(FakeModule2::class.java, Mockito.withSettings().useConstructor(loader, server, name, secrets).defaultAnswer(Answers.CALLS_REAL_METHODS))

    private interface TestService1: ModuleAPI
    private interface TestService2: ModuleAPI
    private interface TestService3: ModuleAPI

    private abstract class FakeModule1(moduleLoader: ModuleLoader, server: Server, name: String, secrets: ModuleLoader.LoadingSecrets) : TestService1,
        Module(moduleLoader, server, name, secrets)
    private abstract class FakeModule2(moduleLoader: ModuleLoader, server: Server, name: String, secrets: ModuleLoader.LoadingSecrets) : TestService2,
        Module(moduleLoader, server, name, secrets)

    private abstract class FakeModuleLoader : ModuleLoader() {
        override val version: String
            get() = "version"
        override val description: String
            get() = "description"
        override val authors: Set<ContactInformation>
            get() = emptySet()
        override val sourceCode: URL
            get() = URL("https://github.com/AsyncMC/")

        public abstract override fun CoroutineScope.createModules(server: Server, secrets: LoadingSecrets): List<Module>
    }
}
