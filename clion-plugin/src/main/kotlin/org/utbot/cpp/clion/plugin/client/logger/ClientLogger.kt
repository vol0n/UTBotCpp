package org.utbot.cpp.clion.plugin.client.logger

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service
class ClientLogger(project: Project) {
    init {
        System.err.println("Client logger constructor is called!")
    }
    var level = LogLevel.TRACE
        set(value) {
            info { "Setting new log level: ${value.text}" }
            field = value
        }

    val logWriters: MutableList<LogWriter> = init(project)

    private fun init(project: Project): MutableList<LogWriter> {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            println("IN UNIT TEST MODE SETTING LOG WRITER")
            return mutableListOf(SystemWriter())
        }
        else return mutableListOf(ConsoleWriter(project))
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
