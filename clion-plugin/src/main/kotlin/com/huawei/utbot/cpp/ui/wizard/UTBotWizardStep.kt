package com.huawei.utbot.cpp.ui.wizard

import com.intellij.ide.wizard.Step
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon
import javax.swing.JComponent

object MyIcons {
    @JvmField
    val UTBOT = IconLoader.getIcon("/icons/utbotIcon.png", javaClass)
}

abstract class UTBotWizardStep: Step {
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