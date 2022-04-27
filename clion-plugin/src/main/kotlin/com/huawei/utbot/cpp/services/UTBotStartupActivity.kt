package com.huawei.utbot.cpp.services

import com.huawei.utbot.cpp.client.Client
import com.huawei.utbot.cpp.ui.wizard.UTBotWizard
import com.huawei.utbot.cpp.utils.invokeOnEdt
import com.intellij.ide.util.RunOnceUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class UTBotStartupActivity: StartupActivity {
    override fun runActivity(project: Project) {
        // start plugin and connect to server on project opening
        project.service<Client>()
        showWizardOnFirstProjectOpen(project)
    }

    private fun showWizardOnFirstProjectOpen(project: Project) {
        RunOnceUtil.runOnceForProject(project, "Show UTBot Wizard") {
            invokeOnEdt {
                UTBotWizard(project).showAndGet()
            }
        }
    }
}