package com.huawei.utbot.cpp.actions

import com.huawei.utbot.cpp.actions.utils.getProjectRequestMessage
import com.huawei.utbot.cpp.client.Requests.LineRequest
import com.huawei.utbot.cpp.client.Requests.ProjectRequest
import com.huawei.utbot.cpp.utils.client
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class GenerateForProjectAction : GenerateTestsBaseAction() {
    override fun updateIfServerAvailable(e: AnActionEvent) {
        e.presentation.isEnabled = (e.project != null)
    }

    override fun actionPerformed(e: AnActionEvent) {
        ProjectRequest(
            getProjectRequestMessage(e),
            e.project!!,
            "Generate for line..."
        ).apply {
            e.client.execute(this)
        }
    }
}