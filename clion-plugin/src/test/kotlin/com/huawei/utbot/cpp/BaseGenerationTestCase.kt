package com.huawei.utbot.cpp

import com.huawei.utbot.cpp.client.Client
import com.huawei.utbot.cpp.models.UTBotTarget
import com.huawei.utbot.cpp.services.UTBotSettings
import com.huawei.utbot.cpp.services.UTBotStartupActivity
import com.huawei.utbot.cpp.ui.UTBotTargetsController
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl
import com.intellij.util.io.delete
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
     * each time setUp is called, also it may be expensive to copy all project files to temporary directory.
     * This class solves the problem, by using [testsDirectory]
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
        stopKoin()
        UTBotStartupActivity.isTestMode = true
    }

    val projectPath: Path = Paths.get(File(".").canonicalPath).resolve("../integration-tests/c-example").normalize()
    val testsDirectoryPath: Path = projectPath.resolve("cl-plugin-test-tests")
    val buildDirectoryPath: Path = projectPath.resolve("build")

    val fixture: CodeInsightTestFixture = createFixture()

    val projectName: String
        get() = projectPath.last().toString()
    val buildDirName: String
        get() = buildDirectoryPath.last().toString()
    val project: Project
        get() = fixture.project
    val settings: UTBotSettings
        get() = project.service()
    val client: Client
        get() = project.service()

    /*
    override fun createTempDirTestFixture(): TempDirTestFixture {
        return TestFixtureProxy(testProjectPath)
    }
     */

    private fun createFixture(): CodeInsightTestFixture {
        println("Creating fixture")
        val fixture = IdeaTestFixtureFactory.getFixtureFactory().let {
            it.createCodeInsightFixture(it.createFixtureBuilder(projectName, projectPath, false).fixture, TestFixtureProxy(projectPath))
        }
        fixture.setUp()
        fixture.testDataPath = projectPath.toString()
        println("Finished creating fixture")
        return fixture
    }

    fun setTarget(targetName: String) {
        val utbotTarget: UTBotTarget = UTBotTargetsController(project).let { controller ->
            // wait until client is connected to server, and fetches project targets
            runBlocking {
                withTimeout(5000L) {
                    while (!client.isServerAvailable()) {
                        delay(500L)
                    }
                    waitForRequestsToFinish()
                }
                PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
            }
            controller.targets.find { target -> target.name == targetName } ?: error("No such target: $targetName.\n${controller.targets.map { it.name }}")
        }
        settings.targetPath = utbotTarget.path
    }

    // called before each test
    override fun setUp() {
        println("setUP of my UsefulTestcase")
        println("DISPATCHER: ${client.dispatcher}")
        super.setUp()
        settings.buildDirPath = buildDirectoryPath.toString()
        settings.testDirPath = testsDirectoryPath.toString()
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
        println("Building the project with compiler: $compiler, and build dir name: $buildDirName")
        println("BUILD COMMAND: $buildCommand")
        ProcessBuilder("bash", "-c", buildCommand)
            .directory(projectPath.toFile())
            .inheritIO()
            .start()
            .waitFor()

        buildDirectoryPath.assertFileOrDirExists("build directory after building project does not exist!")
    }

    // called after each test
    override fun tearDown() {
        println("tearDown of BaseGenerationTest is called")
        // somehow project service Client is not disposed automatically by the ide, and the exception is thrown that
        // timer related to heartbeat is not disposed. So let's dispose it manually.
        client.dispose()
        stopKoin()
        buildDirectoryPath.delete(recursively = true)
        testsDirectoryPath.delete(recursively = true)
        super.tearDown()
        println("tearDown of BaseGenerationTest has finished!")
    }
}
