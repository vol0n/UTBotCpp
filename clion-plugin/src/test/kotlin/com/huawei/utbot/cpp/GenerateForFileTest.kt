package com.huawei.utbot.cpp

import org.junit.jupiter.api.Test
import org.tinylog.kotlin.Logger

class GenerateForFileTest : BaseGenerationTestCase() {
    init {
        Logger.trace("GenerateForFileTest init block is called!")
    }

    @Test
    fun testGenerateForFile() {
        buildProject(compiler = Compiler.Clang, buildDirName = buildDirName)
        fixture.configureFromTempProjectFile("/lib/basic_functions.c")
        fixture.performEditorAction("com.huawei.utbot.cpp.actions.GenerateForFileActionInEditor")

        waitForRequestsToFinish()

        testsDirectoryPath.assertFileOrDirExists()
        testsDirectoryPath.resolve("lib/basic_functions_test.cpp").assertFileOrDirExists()
        testsDirectoryPath.assertAllFilesNotEmptyRecursively()
    }
}
