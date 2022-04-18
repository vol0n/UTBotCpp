package com.huawei.utbot.cpp.ui.wizard

import com.huawei.utbot.cpp.client.Client
import com.huawei.utbot.cpp.services.UTBotSettings
import com.huawei.utbot.cpp.utils.generatorSettings
import com.huawei.utbot.cpp.utils.utbotSettings
import com.intellij.ide.wizard.Step
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.htmlComponent
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
}

class IntroStrep : UTBotWizardStep() {
    override fun createComponent(): JComponent {
        return panel {
            row {
                val html = """
                        <h2>👋Welcome to "UTBot: Quickstart" Wizard! </h2>
                        <p> UTBot discovered that this is the first time you use it with this project.
                        The Wizard will help you to configure the extension appropriatly.
                        In case you don't wish to proceed, you can close this wizard at any time. </p>
                        <p> In order to learn more about UTBot C/C++, please, refer to this <a href="https://github.com/UnitTestBot/UTBotCpp/wiki">manual</a>. </p>
                """.trimIndent()
                cell(htmlComponent(html))
            }
        }
    }
}

class ServerInstallationStep : UTBotWizardStep() {
    override fun createComponent(): JComponent {
        return panel {
            row {
                val html = """
                    <h2>🖥️Server Installation</h2>
                    <p> If you are working on remote machine you can start UTBot Server installation
                        right from here. Otherwise, please do it manually. </p>
                    <p> In order to learn more about UTBot Server Installation process, 
                        please, refer to the 
                        <a href="https://github.com/UnitTestBot/UTBotCpp/wiki/install-server">installation manual</a>. 
                    </p>
                """.trimIndent()
                cell(htmlComponent(html))
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
                val html = """
                <h2>📶 Connection</h2>
                <p>Fill the parameters below accordingly to the ones specified during the 
                <a href="https://github.com/UnitTestBot/UTBotCpp/wiki/install-server">UTBot Server installation</a>.</p>
            """.trimIndent()
                cell(htmlComponent(html))
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

class RemotePathStep(private val project: Project): UTBotWizardStep() {
    override fun createComponent(): JComponent {
        return panel {
            row {
                val html = """
                    <h2>📁Remote Path</h2>
        <p>Remote path configuration specifies the path to the project on a remote host.</p>
                """.trimIndent()
                cell(htmlComponent(html))
            }
            row {
                textField().bindText(project.utbotSettings::remotePath)
            }
        }
    }
}

class BuildOptionsStep(private val project: Project): UTBotWizardStep() {
    override fun createComponent(): JComponent {
       return panel {
           row {
               val html = """
                    <h2>🏗️Build Directory</h2>
        <p>Relative path to the build directory. Files compile_commands.json and link_commands.json should be located in this directory. </p>
                """.trimIndent()
               cell(htmlComponent(html))
           }
           row {
               textField().bindText(project.utbotSettings::buildDirPath)
           }
           row {
               val html = """
                   <h2>🎌CMake Options</h2>
        <p>Options passed to CMake command. </p>
               """.trimIndent()
               cell(htmlComponent(html))
           }
           row {
               textField().bindText(project.utbotSettings::cmakeOptions)
           }
       }
    }
}

class SuccessStep: UTBotWizardStep() {
    override fun createComponent(): JComponent {
        return panel {
            row {
                val html = """
                    <h2>🎉Success!</h2>
        <p> UTBot extension was successfully configured, and now you are ready to use all its functionality. </p>
        <p> If you want to learn more about UTBot C/C++ or  you have ay questions related to its usage, please, refer to this 
        <a href="https://github.com/UnitTestBot/UTBotCpp/wiki">manual</a>.</p>
        <p> 
                """.trimIndent()
                cell(htmlComponent(html))
            }
        }
    }
}