package com.huawei.utbot.cpp.services

import com.huawei.utbot.cpp.client.Client
import com.huawei.utbot.cpp.ui.OutputType
import com.huawei.utbot.cpp.ui.OutputWindowProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.koin.core.context.startKoin
import org.koin.dsl.module

class UTBotStartupActivity: StartupActivity {
    override fun runActivity(project: Project) {
        setupDependencies(project)
        project.service<Client>()
    }

    private fun setupDependencies(project: Project) {
        println("setupDependencies in UTBotStartupActivity is called")
        val console = project.service<OutputWindowProvider>().outputs[OutputType.CLIENT_LOG]!!
        val clientDependencies = module {
            single { console }
            single { if (isTestMode) Dispatchers.Default else Dispatchers.Swing }
        }
        startKoin {
            modules(clientDependencies)
        }
        println("setupDependencies have finished!")
    }

    companion object {
        var isTestMode = false
    }
}