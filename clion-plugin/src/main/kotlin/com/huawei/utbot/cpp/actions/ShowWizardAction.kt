package com.huawei.utbot.cpp.actions

import com.huawei.utbot.cpp.ui.wizard.UTBotWizard
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ShowWizardAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        UTBotWizard(e.project ?: return).showAndGet()
    }
}