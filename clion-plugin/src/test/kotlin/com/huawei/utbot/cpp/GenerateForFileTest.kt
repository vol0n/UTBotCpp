package com.huawei.utbot.cpp

import java.nio.file.Files

class GenerateForFileTest : BaseGenerationTestCase() {
    fun testGenerateForFile() {
        println("test testGenerateForFile has started!")
        println(testProjectPath)
        println(myFixture.tempDirFixture.tempDirPath)
        println(myFixture.testDataPath)
        myFixture.configureFromTempProjectFile("/lib/basic_functions.c")
        myFixture.performEditorAction("com.huawei.utbot.cpp.actions.GenerateForFileActionInEditor")
        waitForRequestsToFinish()
        Files.list(testProjectTestDir).forEach {
            println(it.toString())
        }
        checkFileExists(testProjectBuildDir, "build dir does not exist")
        checkFileExists(testProjectTestDir, "tests folder does not exist")
        checkFileExists(testProjectTestDir.resolve("lib/basic_functions_test.cpp"), "generated test file does not exist ")
        println("test testGenerateForFile has finished!")
    }
}
