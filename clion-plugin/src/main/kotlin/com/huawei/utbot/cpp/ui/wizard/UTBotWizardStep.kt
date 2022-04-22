package com.huawei.utbot.cpp.ui.wizard

import com.huawei.utbot.cpp.client.Client
import com.huawei.utbot.cpp.utils.utbotSettings
import com.intellij.ide.wizard.Step
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.util.ui.HtmlPanel
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.properties.Delegates
import java.awt.Color
import java.awt.Component
import java.awt.Dimension

abstract class UTBotWizardStep : Step {
    protected val panel by lazy { JPanel() }
    private var isInitialized = false
    private val onApplyCallbacks = mutableListOf<()->Unit>()

    abstract fun createUI()

    override fun _init() {
        if (!isInitialized) {
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            panel.preferredSize = Dimension(800, 400)
            panel.minimumSize = panel.preferredSize
            createUI()
            isInitialized = true
        }
    }

    fun DialogPanel.addToUI() {
        alignmentX = Component.LEFT_ALIGNMENT
        panel.add(this)
        onApplyCallbacks.add { apply() }
    }

    override fun _commit(finishChosen: Boolean) {
        onApplyCallbacks.forEach {
            it.invoke()
        }
    }

    override fun getIcon(): Icon? {
        return null
    }

    override fun getComponent(): JComponent {
        return panel
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return panel
    }

    fun getTextResource(resource: String): String {
        return this.javaClass.classLoader.getResource(resource)?.readText()
            ?: error("Unable to get resource: $resource")
    }

    fun addHtml(htmlResource: String) {
        panel.add(createHtmlComponent(getTextResource(htmlResource)))
    }

    private fun createHtmlComponent(html: String): JComponent {
        return object : HtmlPanel() {
            init {
                update()
                alignmentX = Component.LEFT_ALIGNMENT
                adjustHeightToTextHeight()
            }
            override fun getBody() = html

            fun adjustHeightToTextHeight() {
                // set dummy size, to update preferred
                size = Dimension(100, Short.MAX_VALUE.toInt())
                size = preferredSize
                minimumSize = preferredSize
                maximumSize = preferredSize
                update()
            }
        }
    }
}

class IntroStrep : UTBotWizardStep() {
    override fun createUI() {
        addHtml("media/intro.html")
    }
}

class ServerInstallationStep : UTBotWizardStep() {
    override fun createUI() {
        addHtml("media/installation.html")
        panel {
            row {
                button("Install") {
                    // todo: what script should be executed here?
                }
            }
        }.apply {
            addToUI()
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

    override fun createUI() {
        addHtml("media/connection.html")
        panel {
            row("Host") {
                textField().also {
                    it.bindText(project.utbotSettings::serverName)
                    hostTextField = it.component
                }.columns(COLUMNS_MEDIUM)
            }
            row("Port") {
                intTextField().also {
                    it.bindIntText(project.utbotSettings::port)
                    portTextField = it.component
                }.columns(COLUMNS_MEDIUM)
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
        }.addToUI()
    }
}

class RemotePathStep(private val project: Project) : UTBotWizardStep() {
    override fun createUI() {
        addHtml("media/remote_path.html")
        panel {
            row {
                textField()
                    .bindText(project.utbotSettings::remotePath)
                    .columns(COLUMNS_LARGE)
            }
        }.addToUI()
    }
}

class BuildOptionsStep(private val project: Project) : UTBotWizardStep() {
    override fun createUI() {
        addHtml("media/build_dir.html")
        panel {
            row {
                textField()
                    .bindText(project.utbotSettings::buildDirPathRelative)
                    .columns(COLUMNS_LARGE)
            }
        }.addToUI()
        addHtml("media/cmake_options.html")
        panel {
            row {
                textField()
                    .bindText(project.utbotSettings::cmakeOptions)
                    .columns(COLUMNS_LARGE)
            }
        }.addToUI()
    }
}

class SuccessStep : UTBotWizardStep() {
    override fun createUI() {
        addHtml("media/success.html")
    }
}
