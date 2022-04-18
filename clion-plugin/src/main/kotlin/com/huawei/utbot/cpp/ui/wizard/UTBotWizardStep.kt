package com.huawei.utbot.cpp.ui.wizard

import com.huawei.utbot.cpp.client.Client
import com.huawei.utbot.cpp.utils.utbotSettings
import com.intellij.ide.wizard.Step
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.htmlComponent
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ComponentPredicate
import javax.swing.Icon
import javax.swing.JComponent
import kotlin.properties.Delegates

object MyIcons {
    @JvmField
    val UTBOT = IconLoader.getIcon("/icons/utbotIcon.png", javaClass)
}

abstract class UTBotWizardStep : Step {
    override fun _init() {}
    val mainComponent by lazy { createComponent() }

    override fun _commit(finishChosen: Boolean) {}

    override fun getIcon(): Icon {
        return MyIcons.UTBOT
    }

    abstract fun createComponent(): JComponent

    override fun getComponent(): JComponent {
        return mainComponent
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return mainComponent
    }

    fun getTextResource(resource: String): String {
        return this.javaClass.classLoader.getResource(resource)?.readText() ?: error("Unable to get resource: $resource")
    }

    fun Row.addHtml(htmlResource: String) {
        this.cell(htmlComponent(getTextResource(htmlResource)))
    }
}

class IntroStrep : UTBotWizardStep() {
    override fun createComponent(): JComponent {
        return panel {
            row {
                addHtml("media/intro.html")
            }
        }
    }
}

class ServerInstallationStep : UTBotWizardStep() {
    override fun createComponent(): JComponent {
        return panel {
            row {
                addHtml("media/installation.html")
            }
            row {
                button("Install") {
                    // todo: what script should be executed here?
                }
            }
        }
    }
}

class ConnectionStep(val project: Project) : UTBotWizardStep() {
    lateinit var hostTextField: JBTextField
    lateinit var portTextField: JBTextField
    private val pingListeners = mutableListOf<(Boolean?) -> Unit>()
    private var pingedServer: Boolean? by Delegates.observable(null) { _, _, newValue ->
        pingListeners.forEach { listener ->
            listener.invoke(newValue)
        }
    }
    private val pingStateListeners = mutableListOf<(Boolean) -> Unit>()
    private var isPingingServer: Boolean by Delegates.observable(false) { _, _, newValue ->
        pingStateListeners.forEach { listener ->
            listener(newValue)
        }
    }

    override fun createComponent(): JComponent {
        return panel {
            row {
                addHtml("media/connection.html")
            }
            row("Host") {
                textField().also {
                    it.bindText(project.utbotSettings::serverName)
                    hostTextField = it.component
                }
            }
            row("Port") {
                intTextField().also {
                    it.bindIntText(project.utbotSettings::port)
                    portTextField = it.component
                }
            }
            row {
                button("Test Connection") {
                    isPingingServer = true
                    project.service<Client>().pingServer(portTextField.text.toInt(), hostTextField.text, onSuccess = {
                        pingedServer = true
                        isPingingServer = false
                    }, onFailure = {
                        pingedServer = false
                        isPingingServer = false
                        Messages.showErrorDialog(null as Project?, it.message, "Ping Server Failed!")
                    })
                }

                cell(JBLabel(com.intellij.ui.AnimatedIcon.Default())).visibleIf(object : ComponentPredicate() {
                    override fun invoke() = isPingingServer
                    override fun addListener(listener: (Boolean) -> Unit) {
                        pingStateListeners.add(listener)
                    }
                })
                label("Successfully pinged the server!").visibleIf(object : ComponentPredicate() {
                    override fun invoke() = pingedServer == true
                    override fun addListener(listener: (Boolean) -> Unit) {
                        pingListeners.add { listener(it == true) }
                    }
                })
                label("Unable to ping the server!").visibleIf(object : ComponentPredicate() {
                    override fun invoke() = pingedServer == false
                    override fun addListener(listener: (Boolean) -> Unit) {
                        pingListeners.add { listener(pingedServer == false) }
                    }
                })
            }
        }
    }
}

class RemotePathStep(private val project: Project) : UTBotWizardStep() {
    override fun createComponent(): JComponent {
        return panel {
            row {
                addHtml("media/remote_path.html")
            }
            row {
                textField().bindText(project.utbotSettings::remotePath)
            }
        }
    }
}

class BuildOptionsStep(private val project: Project) : UTBotWizardStep() {
    override fun createComponent(): JComponent {
        return panel {
            row {
                addHtml("media/build_dir.html")
            }
            row {
                textField().bindText(project.utbotSettings::buildDirPath)
            }
            row {
                addHtml("media/cmake_options.html")
            }
            row {
                textField().bindText(project.utbotSettings::cmakeOptions)
            }
        }
    }
}

class SuccessStep : UTBotWizardStep() {
    override fun createComponent(): JComponent {
        return panel {
            row {
                addHtml("media/success.html")
            }
        }
    }
}