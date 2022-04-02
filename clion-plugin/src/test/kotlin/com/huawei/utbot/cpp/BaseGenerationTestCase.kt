package com.huawei.utbot.cpp

import com.huawei.utbot.cpp.client.Client
import com.huawei.utbot.cpp.models.UTBotTarget
import com.huawei.utbot.cpp.services.UTBotSettings
import com.huawei.utbot.cpp.ui.UTBotTargetsController
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.io.delete
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.core.context.stopKoin
import org.tinylog.kotlin.Logger
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SwingEdtInterceptor::class)
abstract class BaseGenerationTestCase {
    /**
     * Implementation of TempDirTestFixture that uses [testsDirectory] as
     * a tempDirectory, and does not delete it on tearDown.
     *
     * Intellij Platform tests are based on files in temp directory, which is provided and managed by TempDirTestFixture.
     * On tearDown, temp directory is deleted.
     * it may be expensive to copy all project files to temporary directory.
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
        Logger.trace("Init block of base test case is called!!!")
        stopKoin()
        Client.IS_TEST_MODE = true
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

    init {
        settings.buildDirPath = buildDirectoryPath.toString()
        settings.testDirPath = testsDirectoryPath.toString()
    }

    private fun createFixture(): CodeInsightTestFixture {
        Logger.trace("Creating fixture")
        val fixture = IdeaTestFixtureFactory.getFixtureFactory().let {
            it.createCodeInsightFixture(it.createFixtureBuilder(projectName, projectPath, false).fixture, TestFixtureProxy(projectPath))
        }
        fixture.setUp()
        fixture.testDataPath = projectPath.toString()
        Logger.trace("Finished creating fixture")
        return fixture
    }

    fun setTarget(targetName: String) {
        val utbotTarget: UTBotTarget = UTBotTargetsController(project).let { controller ->
            waitForRequestsToFinish()
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
            controller.targets.find { target -> target.name == targetName } ?: error("No such target: $targetName.\n${controller.targets.map { it.name }}")
        }
        settings.targetPath = utbotTarget.path
    }


    // requests to server are asynchronous, need to wait for server to respond
    fun waitForRequestsToFinish() = runBlocking {
        Logger.trace("Waiting for requests to finish")
        while (client.shortLivingRequestsCS.coroutineContext.job.children.toList().isNotEmpty()) {
            delay(500L)
        }
        Logger.trace("Finished waiting!")
    }

    protected fun buildProject(compiler: Compiler, buildDirName: String) {
        val buildCommand = getBuildCommand(compiler, buildDirName)
        Logger.trace("Building the project with compiler: $compiler, and build dir name: $buildDirName")
        Logger.trace("BUILD COMMAND: $buildCommand")
        ProcessBuilder("bash", "-c", buildCommand)
            .directory(projectPath.toFile())
            .start()
            .waitFor()

        Logger.trace("BUILDING FINISHED!")
        buildDirectoryPath.assertFileOrDirExists("build directory after building project does not exist!")
    }

    @AfterEach
    fun tearDown() {
        Logger.trace("tearDown is called!")
        buildDirectoryPath.delete(recursively = true)
        testsDirectoryPath.delete(recursively = true)
    }

    @AfterAll
    fun tearDownAll() {
        Logger.trace("tearDownAll of BaseGenerationTest is called")
        // somehow project service Client is not disposed automatically by the ide, and the exception is thrown that
        // timer related to heartbeat is not disposed. So let's dispose it manually.
        // client.dispose()
        waitForRequestsToFinish()
        fixture.tearDown()
        stopKoin()
        Logger.trace("tearDownAll of BaseGenerationTest has finished!")
    }
}
