package com.huawei.utbot.cpp.messaging

import com.huawei.utbot.cpp.services.UTBotSettings
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting
import com.intellij.codeInsight.daemon.impl.analysis.HighlightLevelUtil.forceRootHighlighting
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.util.Pair
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import java.nio.file.Paths

class FileOpenListener: FileEditorManagerListener {
    override fun fileOpenedSync(
        source: FileEditorManager,
        file: VirtualFile,
        editors: Pair<Array<FileEditor>, Array<FileEditorProvider>>
    ) {
        val project = source.project
        val testsPath = Paths.get(project.service<UTBotSettings>().testDirPath)
        if (file.path.startsWith(testsPath.toString())) {
            val provider = PsiManager.getInstance(source.project).findViewProvider(file) ?: return
            val languages = provider.languages
            languages.forEach {
                val psiFile = provider.getPsi(it) ?: return;
                forceRootHighlighting(psiFile, FileHighlightingSetting.SKIP_HIGHLIGHTING)
                InjectedLanguageManager.getInstance(project).dropFileCaches(psiFile)
                DaemonCodeAnalyzer.getInstance(project).restart()
            }
        }
    }
}