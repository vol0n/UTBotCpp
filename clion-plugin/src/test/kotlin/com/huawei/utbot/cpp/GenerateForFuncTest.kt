package com.huawei.utbot.cpp

class GenerateForLineTest: BaseGenerationTestCase() {
    fun testGenerateForLine() {
        buildProject(Compiler.Clang, testProjectBuildDir.last().toString())
        myFixture.configureFromTempProjectFile("/lib/basic_functions.c")
        myFixture.editor.caretModel.moveToOffset(451)
        myFixture.performEditorAction("com.huawei.utbot.cpp.actions.GenerateForLineAction")
        waitForRequestsToFinish()
        checkFileExists(testProjectBuildDir, "build dir does not exist")
        checkFileExists(testProjectTestDir, "tests folder does not exist")
        checkFileExists(testProjectTestDir.resolve("lib/basic_functions_test.cpp"), "generated test file does not exist ")
    }
}