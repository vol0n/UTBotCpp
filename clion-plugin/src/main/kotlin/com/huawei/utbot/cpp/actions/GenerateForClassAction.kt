package com.huawei.utbot.cpp.actions

import com.huawei.utbot.cpp.actions.utils.getClassRequestMessage
import com.huawei.utbot.cpp.utils.client
import com.huawei.utbot.cpp.actions.utils.getContainingClass
import com.huawei.utbot.cpp.client.Requests.ClassRequest
import com.intellij.openapi.actionSystem.AnActionEvent

class GenerateForClassAction : GenerateTestsBaseAction() {
    override fun actionPerformed(e: AnActionEvent) {
        ClassRequest(
            getClassRequestMessage(e),
            e.project!!,
            "Generate for assertion..."
        ).apply {
            e.client.execute(this)
        }
    }

    override fun updateIfServerAvailable(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = (getContainingClass(e) != null)
    }
}
