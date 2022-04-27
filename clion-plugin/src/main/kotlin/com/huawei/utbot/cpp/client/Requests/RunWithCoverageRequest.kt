package com.huawei.utbot.cpp.client.Requests

import com.huawei.utbot.cpp.client.handlers.CoverageAndResultsHandler
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import testsgen.Testgen
import testsgen.TestsGenServiceGrpcKt

class RunWithCoverageRequest(
    val project: Project,
    request: Testgen.CoverageAndResultsRequest
): BaseRequest<Testgen.CoverageAndResultsRequest, Flow<Testgen.CoverageAndResultsResponse>>(request) {
    override val description: String = "Sending request to get tests RESULTS and COVERAGE."
    private val progressName: String = "Get tests results and coverage..."

    override suspend fun Flow<Testgen.CoverageAndResultsResponse>.handle(cancellationJob: Job?) {
        if (cancellationJob?.isActive == true) {
            CoverageAndResultsHandler(
                project,
                this,
                progressName,
                cancellationJob
            )
        }
    }

    override suspend fun TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub.send(cancellationJob: Job?): Flow<Testgen.CoverageAndResultsResponse> {
        return createTestsCoverageAndResult(request)
    }
}