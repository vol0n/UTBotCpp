package org.utbot.cpp.clion.plugin.utils

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.job
import org.utbot.cpp.clion.plugin.client.Client
import org.utbot.cpp.clion.plugin.client.ClientManager
import org.utbot.cpp.clion.plugin.client.logger.ClientLogger
import org.utbot.cpp.clion.plugin.settings.UTBotAllProjectSettings

val Project.logger: ClientLogger
    get() = this.service()

val AnActionEvent.client: Client
    get() = this.getRequiredData(CommonDataKeys.PROJECT).getClient()

fun Project.getClient(): Client = this.service<ClientManager>().client

val CoroutineScope.children
    get() = this.coroutineContext.job.children.toList()

fun CoroutineScope.hasChildren(): Boolean = children.isNotEmpty()

fun invokeOnEdt(task: () -> Unit) {
    ApplicationManager.getApplication().invokeLater(task)
}

val isWindows
    get() = System.getProperty("os.name").contains("win", ignoreCase = true)
