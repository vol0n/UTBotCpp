package com.huawei.utbot.cpp

import org.junit.jupiter.api.Test

class GenerateForIsolatedFileTest : BaseGenerationTestCase() {
    @Test
    fun testGenerateForFile() {
        buildProject(compiler = Compiler.Clang, buildDirName = "build")
        fixture.configureFromTempProjectFile("/lib/basic_functions.c")
        fixture.performEditorAction("com.huawei.utbot.cpp.actions.GenerateForFileActionInEditor")
        waitForRequestsToFinish()
        testsDirectoryPath.assertFileOrDirExists()
        testsDirectoryPath.resolve("lib/basic_functions_test.cpp").assertFileOrDirExists()
        testsDirectoryPath.assertAllFilesNotEmptyRecursively()
    }
}
