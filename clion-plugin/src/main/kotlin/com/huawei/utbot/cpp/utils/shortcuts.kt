package com.huawei.utbot.cpp.utils

import com.huawei.utbot.cpp.client.Client
import com.huawei.utbot.cpp.services.UTBotSettings
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.job

val AnActionEvent.client: Client
    get() = this.getRequiredData(CommonDataKeys.PROJECT).service()

fun String.convertFromRemotePathIfNeeded(project: Project): String {
    return project.service<UTBotSettings>().convertFromRemotePathIfNeeded(this)
}

fun String.convertToRemotePathIfNeeded(project: Project): String {
    return project.service<UTBotSettings>().convertToRemotePathIfNeeded(this)
}

val CoroutineScope.children
    get() = this.coroutineContext.job.children.toList()

val CoroutineScope.hasChildren
    get() = this.children.isNotEmpty()

