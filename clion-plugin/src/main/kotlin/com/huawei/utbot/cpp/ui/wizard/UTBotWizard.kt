package com.huawei.utbot.cpp.ui.wizard

import com.intellij.ide.wizard.AbstractWizard
import com.intellij.openapi.project.Project

class UTBotWizard(title: String, project: Project): AbstractWizard<UTBotWizardStep>(title, project) {
    init {
        addStep(IntroStrep())
        init()
    }

    override fun getHelpID(): String? = null
}
