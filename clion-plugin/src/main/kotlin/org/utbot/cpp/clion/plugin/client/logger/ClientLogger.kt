package org.utbot.cpp.clion.plugin.client.logger

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service
class ClientLogger(project: Project) {
    var level = LogLevel.TRACE
        set(value) {
            info { "Setting new log level: ${value.text}" }
            field = value
        }

    private val logWriters: MutableList<LogWriter> = init(project)

    private fun init(project: Project): MutableList<LogWriter> {
        return if (ApplicationManager.getApplication().isUnitTestMode) {
            mutableListOf(SystemWriter())
        } else mutableListOf(ConsoleWriter(project))
    }

    fun info(message: String) = log({ message }, LogLevel.INFO)
    fun info(message: () -> String) = log(message, LogLevel.INFO)

    fun warn(message: () -> String) = log(message, LogLevel.WARN)

    fun error(message: () -> String) = log(message, LogLevel.ERROR)

    fun debug(message: () -> String) = log(message, LogLevel.DEBUG)

    fun trace(message: () -> String) = log(message, LogLevel.TRACE)

    private fun log(messageSupplier: () -> (String), level: LogLevel, depth: Int = 3) {
        if (level.ordinal < this.level.ordinal){
            return
        }

        val logMessage = LogMessage(messageSupplier, level, Thread.currentThread().stackTrace[depth + 1])
        for (writer in logWriters) {
            writer.write(logMessage)
        }
    }
}
