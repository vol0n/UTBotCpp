package com.huawei.utbot.cpp

import com.huawei.utbot.cpp.client.Client
import com.huawei.utbot.cpp.services.UTBotSettings
import com.huawei.utbot.cpp.services.UTBotStartupActivity
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl
import com.intellij.util.io.delete
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.koin.core.context.stopKoin
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

abstract class BaseGenerationTestCase : UsefulTestCase() {
    /**
     * Implementation of TempDirTestFixture that uses [testsDirectory] as
     * a tempDirectory, and does not delete it on tearDown.
     *
     * Intellij Platform tests are based on files in temp directory, which is provided and managed by TempDirTestFixture.
     * On tearDown, temp directory is deleted.
     * The problem with temp directory that it is not mounted to docker as it is generated
     * each time setUp is called. So if server is inside docker container the project lying in
     * temp directory can't be accessed by server. This class solves the problem, by using [testsDirectory]
     * instead of some generated temp directory.
     */
    class TestFixtureProxy(val testsDirectory: Path): TempDirTestFixtureImpl() {
        override fun doCreateTempDirectory(): Path {
            return testsDirectory
        }

        // as the directory is not actually temporary, it should not be deleted
        override fun deleteOnTearDown() = false
    }

    init {
        println("THE CONSTRUCTOR OF BASEGENERATIONTESTCASE WAS CALLED!")
        stopKoin()
        UTBotStartupActivity.isTestMode = true
    }

    val relativeProjectPath = "../integration-tests/c-example"
    val testProjectPath = Paths.get(File(".").canonicalPath).resolve(relativeProjectPath).normalize()
    val testProjectName = Paths.get(relativeProjectPath).last().toString()
    val testProjectTestDir = testProjectPath.resolve("cl-plugin-test-tests")
    val testProjectBuildDir = testProjectPath.resolve("cl-plugin-test-buildDir")

    val myFixture: CodeInsightTestFixture = createFixture()
    val project: Project = myFixture.project
    val settings: UTBotSettings = project.service()
    val client: Client = project.service()

    private fun createFixture(): CodeInsightTestFixture {
        println("Creating fixture")
        val fixture = IdeaTestFixtureFactory.getFixtureFactory().let {
            it.createCodeInsightFixture(it.createFixtureBuilder(testProjectName, testProjectPath, false).fixture, TestFixtureProxy(testProjectPath))
        }
        fixture.setUp()
        fixture.testDataPath = testProjectPath.toString()
        println("Finished creating fixture")
        return fixture
    }

    // called before each test
    override fun setUp() {
        println("setUP of my UsefulTestcase")
        println("DISPATCHER: ${client.dispatcher}")
        super.setUp()
        settings.buildDirPath = testProjectBuildDir.toString()
        settings.testDirPath = testProjectTestDir.toString()
        println("setUp of my UsefulTestCase has finished!")
    }

    // requests to server are asynchronous, need to wait for server to respond
    fun waitForRequestsToFinish() = runBlocking {
        println("Waiting for requests to finish")
        while (client.shortLivingRequestsCS.coroutineContext.job.children.toList().isNotEmpty()) {
            delay(500L)
        }
        println("Finished waiting!")
    }

    protected fun buildProject(compiler: Compiler, buildDirName: String) {
        val buildCommand = getBuildCommand(compiler, buildDirName)
        println("Building the project with compler: $compiler, and build dir name: $buildDirName")
        println("BUILD COMMAND: $buildCommand")
        ProcessBuilder("bash", "-c", buildCommand)
            .directory(testProjectPath.toFile())
            .inheritIO()
            .start()
            .waitFor()

        checkFileExists(testProjectBuildDir, "Build folder does not exist, after generation")
    }

    // called after each test
    override fun tearDown() {
        println("tearDown of myUsefulTestCase is called")
        // somehow project service Client is not disposed automatically by the ide, and the exception is thrown that
        // timer related to heartbeat is not disposed. So let's dispose it manually.
        client.dispose()
        stopKoin()
        testProjectBuildDir.delete(recursively = true)
        testProjectTestDir.delete(recursively = true)
        super.tearDown()
        println("tearDown of myUseFulTestCase has finished!")
    }
}
