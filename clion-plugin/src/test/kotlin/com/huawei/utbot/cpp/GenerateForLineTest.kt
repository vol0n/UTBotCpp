package com.huawei.utbot.cpp

import com.huawei.utbot.cpp.services.GeneratorSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor

class GenerateForLineTest: BaseGenerationTestCase() {

    fun doTest(lineNumber: Int, targetName: String = "liblib.a", compiler: Compiler = Compiler.Clang, isVerbose: Boolean = true) {
        println("Testing generate for line using target: $targetName, compiler: $compiler, verbose mode: $isVerbose, line: $lineNumber")
        buildProject(compiler, buildDirName)
        setTarget(targetName)
        project.service<GeneratorSettings>().verbose = isVerbose

        fixture.configureFromTempProjectFile("/lib/basic_functions.c")
        fixture.editor.moveCursorToLine(lineNumber)

        fixture.performEditorAction("com.huawei.utbot.cpp.actions.GenerateForLineAction")
        waitForRequestsToFinish()

        testsDirectoryPath.assertFileOrDirExists()
        testsDirectoryPath.resolve("lib/basic_functions_test.cpp").assertFileOrDirExists()
        testsDirectoryPath.assertAllFilesNotEmptyRecursively()
    }

    fun `test generate for head of max line`() {
        doTest(HEAD_OF_MAX_LINE)
    }

    fun `test generate for line if in max function line`() {
        doTest(IF_IN_MAX_FUNCTION_LINE)
    }

    private fun Editor.moveCursorToLine(lineNumber: Int) {
        this.caretModel.moveToOffset(this.document.getLineStartOffset(lineNumber))
    }

    companion object {
        // line numbers are assumed to start from 0
        const val HEAD_OF_MAX_LINE = 6
        const val IF_IN_MAX_FUNCTION_LINE = 7
    }
}