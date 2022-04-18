package com.huawei.utbot.cpp.actions.utils

import com.huawei.utbot.cpp.ui.wizard.UTBotWizard
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ShowWizardAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        UTBotWizard("UTBot quick wizard", e.project ?: return).showAndGet()
    }
}