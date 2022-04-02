package com.huawei.utbot.cpp.services

import com.huawei.utbot.cpp.client.Client
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class UTBotStartupActivity: StartupActivity {
    override fun runActivity(project: Project) {
        // start plugin and connect to server on project opening
        project.service<Client>()
    }
}