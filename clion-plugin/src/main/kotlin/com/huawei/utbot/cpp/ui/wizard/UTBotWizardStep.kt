package com.huawei.utbot.cpp.ui.wizard

import com.intellij.ide.wizard.Step
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.htmlComponent
import com.intellij.ui.layout.panel
import javax.swing.Icon
import javax.swing.JComponent

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
                component(htmlComponent(html))
            }
        }
    }
}