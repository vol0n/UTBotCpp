package com.huawei.utbot.cpp.client.Requests

import com.huawei.utbot.cpp.client.handlers.CheckProjectConfigurationHandler
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import testsgen.Testgen
import testsgen.TestsGenServiceGrpcKt

class CheckProjectConfigurationRequest(
    val project: Project,
    val progressName: String,
    request: Testgen.ProjectConfigRequest,
): BaseRequest<Testgen.ProjectConfigRequest, Flow<Testgen.ProjectConfigResponse>>(request) {
    override val description: String = "Sending request to check project configuration."

    override suspend fun TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub.send(cancellationJob: Job?): Flow<Testgen.ProjectConfigResponse> {
        return this.configureProject(request)
    }

    override suspend fun Flow<Testgen.ProjectConfigResponse>.handle(cancellationJob: Job?) {
        if (cancellationJob?.isActive == true) {
            CheckProjectConfigurationHandler(
                project,
                this,
                progressName,
                cancellationJob
            ).handle()
        }
    }
}
