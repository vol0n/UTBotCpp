package org.utbot.cpp.clion.plugin.ui.targetsToolWindow

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.ui.CollectionListModel
import org.utbot.cpp.clion.plugin.client.requests.ProjectTargetsRequest
import org.utbot.cpp.clion.plugin.grpc.getProjectTargetsGrpcRequest
import org.utbot.cpp.clion.plugin.listeners.ConnectionStatus
import org.utbot.cpp.clion.plugin.listeners.UTBotEventsListener
import org.utbot.cpp.clion.plugin.settings.UTBotAllProjectSettings
import org.utbot.cpp.clion.plugin.settings.settings
import org.utbot.cpp.clion.plugin.utils.getCurrentClient
import org.utbot.cpp.clion.plugin.utils.invokeOnEdt
import org.utbot.cpp.clion.plugin.utils.logger
import org.utbot.cpp.clion.plugin.utils.projectLifetimeDisposable
import testsgen.Testgen

@Service
class UTBotTargetsController(val project: Project) {
    private val settings: UTBotAllProjectSettings
        get() = project.settings

    private val logger get() = project.logger

    private var areTargetsUpToDate = false
    private val targetsUiModel = CollectionListModel(mutableListOf<UTBotTarget>())
    val targetsToolWindow: UTBotTargetsToolWindow by lazy { UTBotTargetsToolWindow(targetsUiModel, this) }

    val targets: List<UTBotTarget>
        get() = targetsUiModel.toList()

    init {
        // requestTargetsFromServer()
        connectToEvents()
    }

    fun isTargetUpToDate(path: String): Boolean = areTargetsUpToDate && targets.find { it.path == path } != null

    fun requestTargetsFromServer() {
        val currentClient = project.getCurrentClient()
        areTargetsUpToDate = false

        invokeOnEdt {
            targetsUiModel.removeAll()
            targetsToolWindow.setBusy(true)
        }
        ProjectTargetsRequest(
            project,
            getProjectTargetsGrpcRequest(project),
            processTargets = { targetsResponse: Testgen.ProjectTargetsResponse ->
                project.logger.info { "Handling project targets response!: $targetsResponse" }
                invokeOnEdt {
                    project.logger.info { "Inside edt block" }
                    targetsToolWindow.setBusy(false)

                    project.logger.info { "Set busy" }
                    targetsUiModel.apply {
                        targetsUiModel.replaceAll(
                            targetsResponse.targetsList.map { projectTarget ->
                                UTBotTarget(projectTarget, project)
                            })
                    }
                    areTargetsUpToDate = true

                    // set selected target in ui
                    val storedTargetPath = project.settings.storedSettings.targetPath
                    var targetToSelect = UTBotTarget.autoTarget
                    if (isTargetUpToDate(storedTargetPath)) {
                        targets.find { it.path == storedTargetPath }?.let {
                            targetToSelect = it
                        }
                    }
                    project.logger.info { "Setting new target" }
                    targetsToolWindow.setSelectedTarget(targetToSelect)
                    project.logger.info { "Finished edt block" }
                }
            },
            onError = {
                project.logger.info { "Error in getProjectTargets occured!" }
                invokeOnEdt {
                    targetsToolWindow.setBusy(false)
                }
            }).let { targetsRequest ->
            if (!currentClient.isServerAvailable()) {
                logger.error { "Could not request targets from server: server is unavailable!" }
                invokeOnEdt {
                    targetsToolWindow.setBusy(false)
                }
                return
            }
            logger.trace { "Requesting project targets from server!" }
            currentClient.executeRequestIfNotDisposed(targetsRequest)
        }
    }


    fun selectionChanged(selectedTarget: UTBotTarget) {
        // when user selects target update model
        settings.storedSettings.targetPath = selectedTarget.path
    }

    fun setTargetPathByName(targetName: String) {
        val target = targets.find { it.name == targetName } ?: error("No such target!")
        settings.storedSettings.targetPath = target.path
    }

    private fun connectToEvents() {
        // this connection is needed during project lifetime so we pass this service as parent disposable
        project.messageBus.connect(project.projectLifetimeDisposable).subscribe(
            // when we reconnected to server, the targets should be updated, so we request them from server
            UTBotEventsListener.CONNECTION_CHANGED_TOPIC,
            object : UTBotEventsListener {
                override fun onConnectionChange(oldStatus: ConnectionStatus, newStatus: ConnectionStatus) {
                    if (newStatus != oldStatus) {
                        requestTargetsFromServer()
                    }
                }
            }
        )
    }
}
