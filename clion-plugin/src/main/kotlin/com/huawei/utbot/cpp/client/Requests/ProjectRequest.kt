package com.huawei.utbot.cpp.client.Requests

import com.intellij.openapi.project.Project
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import testsgen.Testgen
import testsgen.TestsGenServiceGrpcKt

class ProjectRequest(
    request: Testgen.ProjectRequest,
    project: Project,
    progressName: String
) : BaseTestsRequest<Testgen.ProjectRequest>(request, project, progressName) {
    override val description: String = "Sending request to generate for PROJECT."
    override suspend fun TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub.send(cancellationJob: Job?): Flow<Testgen.TestsResponse> =
        generateProjectTests(request)
}
