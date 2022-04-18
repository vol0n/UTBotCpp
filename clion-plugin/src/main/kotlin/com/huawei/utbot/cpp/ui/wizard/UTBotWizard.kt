package com.huawei.utbot.cpp.ui.wizard

import com.intellij.ide.wizard.AbstractWizard
import com.intellij.openapi.project.Project

class UTBotWizard(project: Project): AbstractWizard<UTBotWizardStep>("UTBot: Quickstart", project) {
    init {
        addStep(IntroStrep())
        addStep(ServerInstallationStep())
        addStep(ConnectionStep(project))
        addStep(RemotePathStep(project))
        addStep(BuildOptionsStep(project))
        addStep(SuccessStep())
        init()
    }

    override fun getHelpID(): String? = null
}
