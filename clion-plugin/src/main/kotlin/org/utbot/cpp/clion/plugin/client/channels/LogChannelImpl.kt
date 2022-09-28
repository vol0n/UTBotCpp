package org.utbot.cpp.clion.plugin.client.channels

import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import org.tinylog.kotlin.Logger
import org.utbot.cpp.clion.plugin.ui.userLog.UTBotConsole
import org.utbot.cpp.clion.plugin.utils.invokeOnEdt
import org.utbot.cpp.clion.plugin.utils.logger
import testsgen.Testgen
import testsgen.TestsGenServiceGrpcKt

interface LogChannel {
    suspend fun provide(stub: TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub)
}

abstract class LogChannelImpl(val project: Project): LogChannel {
    abstract val name: String
    abstract val logLevel: String

    abstract val console: UTBotConsole

    abstract suspend fun open(stub: TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub): Flow<Testgen.LogEntry>
    abstract suspend fun close(stub: TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub)

    override fun toString(): String = name

    override suspend fun provide(stub: TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub) {
        val logger = project.logger
        try {
            Logger.trace("Closing log channel: $name")
            close(stub)
            Logger.trace("Finished closing log channel: $name")
        } catch (cause: io.grpc.StatusException) {
            logger.error { "Exception when closing log channel: $name \n$cause" }
        }

        Logger.trace("Opening log channel: $name")
        open(stub)
            .catch { cause -> logger.error{ "Exception in log channel: $name \n$cause" } }
            .collect { invokeOnEdt { console.info(it.message) } }
    }
}