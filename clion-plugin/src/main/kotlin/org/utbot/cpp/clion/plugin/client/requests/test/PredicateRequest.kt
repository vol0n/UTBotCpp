package org.utbot.cpp.clion.plugin.client.requests.test

import com.intellij.openapi.project.Project
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import org.utbot.cpp.clion.plugin.UTBot
import org.utbot.cpp.clion.plugin.grpc.GrpcRequestBuilder
import testsgen.Testgen
import testsgen.TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub

class PredicateRequest(
    params: GrpcRequestBuilder<Testgen.PredicateRequest>,
    project: Project,
) : BaseTestsRequest<Testgen.PredicateRequest>(params, project, UTBot.message("requests.predicate.description.progress")) {
    override val id: String = "Generate for Predicate"

    override val logMessage: String = "Sending request to generate tests for predicate"
    override fun getInfoMessage(): String = "Tests for predicate are generated!"

    override suspend fun TestsGenServiceCoroutineStub.send(cancellationJob: Job?): Flow<Testgen.TestsResponse> =
        generatePredicateTests(request)
}
