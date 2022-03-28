//package com.huawei.utbot.cpp
//
//import com.huawei.utbot.cpp.actions.utils.getProjectTargetsRequest
//
//class GetProjectTargetsTest: BaseGenerationTestCase() {
//    fun testProjectTargetsAreReturned() {
//        buildProject(Compiler.Clang, testProjectBuildDir.last().toString())
//        client.requestProjectTargetsAndProcess(getProjectTargetsRequest(project)) {
//            println("Received project targets from server")
//            assert(it.targetsList.isNotEmpty())
//            it.targetsList.forEach { target ->
//                println(target.name)
//            }
//        }
//        waitForRequestsToFinish()
//    }
//}