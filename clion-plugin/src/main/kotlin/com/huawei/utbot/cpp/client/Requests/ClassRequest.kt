package com.huawei.utbot.cpp.client.Requests

import com.intellij.openapi.project.Project
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import testsgen.Testgen
import testsgen.TestsGenServiceGrpcKt

class ClassRequest(
    request: Testgen.ClassRequest,
    project: Project,
    progressName: String
) : BaseTestsRequest<Testgen.ClassRequest>(request, project, progressName) {
    override val description: String = "Sending request to generate tests for CLASS."
    override suspend fun TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub.send(cancellationJob: Job?): Flow<Testgen.TestsResponse> =
        generateClassTests(request)
}
