package org.utbot.cpp.clion.plugin

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl
import com.intellij.util.io.delete
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.utbot.cpp.clion.plugin.client.Client
import org.utbot.cpp.clion.plugin.settings.settings
import org.utbot.cpp.clion.plugin.ui.targetsToolWindow.UTBotTargetsController
import org.utbot.cpp.clion.plugin.utils.getCurrentClient
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.tinylog.kotlin.Logger

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
    class TestFixtureProxy(private val testsDirectory: Path) : TempDirTestFixtureImpl() {
        override fun doCreateTempDirectory(): Path {
            return testsDirectory
        }

        // as the directory is not actually temporary, it should not be deleted
        override fun deleteOnTearDown() = false
    }

    val projectPath: Path =
        Paths.get(File(".").canonicalPath).resolve("../integration-tests/c-example-mini").normalize()
    val testsDirectoryPath: Path = projectPath.resolve("cl-plugin-test-tests")
    val buildDirName = "build"
    val fixture: CodeInsightTestFixture = createFixture()
    val project: Project
        get() = fixture.project

    init {
        project.settings.storedSettings.buildDirRelativePath = buildDirName
        project.settings.storedSettings.testsDirRelativePath = projectPath.relativize(testsDirectoryPath).toString()
    }

    val client: Client
        get() = project.getCurrentClient()

    private fun createFixture(): CodeInsightTestFixture {
        // can't use Logger
        println("Creating fixture")
        val fixture = IdeaTestFixtureFactory.getFixtureFactory().let {
            it.createCodeInsightFixture(
                it.createFixtureBuilder(projectPath.name, projectPath, false).fixture,
                TestFixtureProxy(projectPath)
            )
        }
        fixture.setUp()
        fixture.testDataPath = projectPath.toString()
        println("Finished creating fixture")
        return fixture
    }

    fun waitForConnection(timeout: Long = 10000L) {
        runBlocking {
            try {
                withTimeout(timeout) {
                    while (!client.isServerAvailable()) {
                        delay(1000L)
                        Logger.info { "Waiting for connection to server!" }
                    }
                }
            } catch (_: TimeoutCancellationException) {}
            assert(client.isServerAvailable()) { "Not connected to server!" }
            Logger.info { "Connected" }
        }
    }

    fun setTarget(targetName: String) {
        Logger.info { "Setting new target during test: $targetName" }
        val targetsController: UTBotTargetsController = project.service<UTBotTargetsController>()
        waitForConnection()
        targetsController.requestTargetsFromServer()
        waitForRequestsToFinish()
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        targetsController.setTargetPathByName(targetName)
    }

    /**
     * Waits until all requests initiated during tests are finished
     */
    fun waitForRequestsToFinish() {
        // requests to server are asynchronous, need to wait for server to respond
        Logger.info { "Started waiting for server requests!" }
        client.waitForServerRequestsToFinish(ifNotFinished = { unfinishedCoroutines: List<Job> ->
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
            Logger.info("Waiting for requests to finish: $unfinishedCoroutines")
        })
        Logger.info("Finished waiting!")
    }

    @AfterEach
    fun tearDown() {
        Logger.info("tearDown is called!")
        project.settings.buildDirPath.delete(recursively = true)
        testsDirectoryPath.delete(recursively = true)
    }

    @AfterAll
    fun tearDownAll() {
        Logger.info("tearDownAll of BaseGenerationTest is called")
        fixture.tearDown()
        Logger.info("tearDownAll of BaseGenerationTest has finished!")
    }
}
