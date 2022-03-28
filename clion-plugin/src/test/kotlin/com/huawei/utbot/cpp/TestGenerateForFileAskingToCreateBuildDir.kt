package com.huawei.utbot.cpp

import java.nio.file.Files

class GenerateForFileAskingToCreateBuildDirTest: BaseGenerationTestCase() {
    fun testGenerateForFileAskingToGenerateBuildFolder() {
        client.createBuildDir()
        waitForRequestsToFinish()
        client.generateJSon()
        waitForRequestsToFinish()
        client.configureProject()
        waitForRequestsToFinish()
        myFixture.configureFromTempProjectFile("/lib/basic_functions.c")
        myFixture.performEditorAction("com.huawei.utbot.cpp.actions.GenerateForFileActionInEditor")
        waitForRequestsToFinish()
        Files.list(testProjectTestDir).forEach {
            println(it.toString())
        }
        checkFileExists(testProjectBuildDir, "build dir does not exist")
        checkFileExists(testProjectTestDir, "tests folder does not exist")
        checkFileExists(testProjectTestDir.resolve("lib/basic_functions_test.cpp"), "generated test file does not exist ")
    }
}