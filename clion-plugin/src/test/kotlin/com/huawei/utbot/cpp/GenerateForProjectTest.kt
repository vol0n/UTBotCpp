package com.huawei.utbot.cpp

import com.huawei.utbot.cpp.actions.GenerateForProjectAction
import org.junit.jupiter.api.Test

class GenerateForProjectTest: BaseGenerationTestCase() {
    @Test
    fun testGenerateForProject() {
        buildProject(Compiler.Clang, buildDirName)
        fixture.testAction(GenerateForProjectAction())
        waitForRequestsToFinish()
        testsDirectoryPath.resolve("lib").assertFilesExist(
            "assertion_failures", "basic_functions",
            "dependent_functions", "inner_basic_functions",
            "complex_structs", "main", "pointer_return",
            "struct_arrays", "pointer_parameters",
            "simple_structs", "types", "typedefs_1", "simple_unions",
            "typedefs_2", "enums", "packed_structs", "void_functions",
            "floating_point", "floating_point_plain", "helloworld", "halt",
            "bits", "types_2", "keywords", "qualifiers", "static", "structs_with_pointers",
            "function_pointers", "globals", "multi_arrays", "alignment", "types_3", "memory", "calc",
            "variadic", "symbolic_stdin", "libfunc", "linked-list", "tree"
        )
    }
}