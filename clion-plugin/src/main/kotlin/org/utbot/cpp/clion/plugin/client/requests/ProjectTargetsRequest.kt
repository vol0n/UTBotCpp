package org.utbot.cpp.clion.plugin.client.requests

import com.intellij.openapi.project.Project
import kotlinx.coroutines.Job
import org.utbot.cpp.clion.plugin.utils.logger
import testsgen.Testgen
import testsgen.TestsGenServiceGrpcKt

class ProjectTargetsRequest(
    project: Project,
    request: Testgen.ProjectTargetsRequest,
    val processTargets: suspend (Testgen.ProjectTargetsResponse)->Unit,
    val onError: suspend (Throwable) -> Unit
): BaseRequest<Testgen.ProjectTargetsRequest, Testgen.ProjectTargetsResponse>(request, project) {

    override val logMessage: String = "Sending request to get project targets"

    override suspend fun execute(stub: TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub, cancellationJob: Job?) {
        try {
            project.logger.info { "Before executing targets request!"}
            super.execute(stub, cancellationJob)
            project.logger.info { "After executing targets request!"}
        } catch (e: Throwable) {
            project.logger.info { "Error on project targets reqeust: ${e.message}!"}
            onError(e)
        }
    }

    override suspend fun Testgen.ProjectTargetsResponse.handle(cancellationJob: Job?) = processTargets(this)

    override suspend fun TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub.send(cancellationJob: Job?): Testgen.ProjectTargetsResponse =
        getProjectTargets(request)
}
