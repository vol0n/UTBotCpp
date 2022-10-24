package org.utbot.cpp.clion.plugin.client.channels

import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import org.utbot.cpp.clion.plugin.ui.utbotToolWindow.logsToolWindow.UTBotConsole
import org.utbot.cpp.clion.plugin.utils.invokeOnEdt
import org.utbot.cpp.clion.plugin.utils.logger
import testsgen.Testgen
import testsgen.TestsGenServiceGrpcKt

interface LogChannel {
    suspend fun provide(stub: TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub)
}

abstract class LogChannelImpl(val project: Project) : LogChannel {
    abstract val name: String
    abstract val logLevel: String

    // because this function is called asynchronously (we call it from invokeOnEdt which is async)
    // it may be called when project is disposed or other services are disposed -- this is very unlikely in production
    // but possible in tests, so we do nothing if this happens
    private val console: UTBotConsole? by lazy { if (project.isDisposed) null else createConsole() }

    abstract suspend fun open(stub: TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub): Flow<Testgen.LogEntry>
    abstract suspend fun close(stub: TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub)

    abstract fun createConsole(): UTBotConsole?

    override fun toString(): String = name

    override suspend fun provide(stub: TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub) {
        val logger = project.logger
        try {
            close(stub)
        } catch (cause: io.grpc.StatusException) {
            logger.error { "Exception when closing log channel: $name \n$cause" }
        }

        open(stub)
            .catch { cause -> logger.error { "Exception in log channel: $name \n$cause" } }
            .collect { invokeOnEdt { console?.info(it.message) } }
    }
}
