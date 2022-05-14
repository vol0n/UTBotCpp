package com.huawei.utbot.cpp.client

import com.huawei.utbot.cpp.messaging.SourceFoldersListener
import com.huawei.utbot.cpp.messaging.UTBotSettingsChangedListener
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlin.random.Random
import org.tinylog.kotlin.Logger

@Service
class ClientManager(val project: Project) {
    private val clientId = generateClientID()
    var client: Client = Client(project, clientId)
        private set

    init {
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        with(project.messageBus.connect()) {
            subscribe(UTBotSettingsChangedListener.TOPIC, UTBotSettingsChangedListener { newSettings ->
                if (newSettings.port != client.port || newSettings.serverName != client.serverName) {
                    Logger.trace("Connection settings changed. Setting up new client.")
                    client.dispose()
                    client = Client(project, clientId)
                }
            })
            subscribe(
                SourceFoldersListener.TOPIC,
                // when source folder are changed, the ProjectViewNodeDecorator.decorate should be invoked again for this we force refresh on change
                SourceFoldersListener {
                    ProjectView.getInstance(project).refresh()
                })
        }
    }

    private fun generateClientID(): String {
        fun createRandomSequence() = (1..RANDOM_SEQUENCE_LENGTH)
            .joinToString("") { Random.nextInt(0, RANDOM_SEQUENCE_MAX_VALUE).toString() }

        return "${(System.getenv("USER") ?: "unknownUser")}-${createRandomSequence()}"
    }

    companion object {
        const val RANDOM_SEQUENCE_MAX_VALUE = 10
        const val RANDOM_SEQUENCE_LENGTH = 5
    }
}
