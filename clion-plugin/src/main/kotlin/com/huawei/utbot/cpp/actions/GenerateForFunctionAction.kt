package com.huawei.utbot.cpp.actions

import com.huawei.utbot.cpp.actions.utils.getFunctionRequestMessage
import com.huawei.utbot.cpp.utils.client
import com.huawei.utbot.cpp.actions.utils.getContainingFunction
import com.huawei.utbot.cpp.client.Requests.FolderRequest
import com.huawei.utbot.cpp.client.Requests.FunctionRequest
import com.intellij.openapi.actionSystem.AnActionEvent

class GenerateForFunctionAction : GenerateTestsBaseAction() {
    override fun updateIfServerAvailable(e: AnActionEvent) {
        FunctionRequest(
            getFunctionRequestMessage(e),
            e.project!!,
            "Generate for function..."
        ).apply {
            e.client.execute(this)
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.client.generateForFunction(getFunctionRequestMessage(e))
    }
}
