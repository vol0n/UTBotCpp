package com.huawei.utbot.cpp

import com.intellij.util.io.exists
import com.intellij.util.io.readText
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

enum class Compiler {
    Clang, Gcc
}

fun Path.assertFilesExist(vararg fileNames: String) {
    for (fileName in fileNames) {
        this.resolve("${fileName}_test.cpp").assertFileOrDirExists()
    }
}

fun getBuildCommand(compiler: Compiler, buildDirName: String): String {
    val bear = "/utbot_distr/bear/bin/bear"
    val cmake = "/utbot_distr/install/bin/cmake"
    val clang = "/utbot_distr/install/bin/clang"
    val clangpp = "/utbot_distr/install/bin/clang++"

    return when (compiler) {
        Compiler.Clang -> "export CC=$clang && export CXX=$clangpp && " +
                "mkdir -p $buildDirName && cd $buildDirName && $cmake .. && $bear make -j8"
        Compiler.Gcc -> "export C_INCLUDE_PATH=\"\" && export CC=gcc && export CXX=g++ && " +
                "mkdir -p $buildDirName && cd $buildDirName && $cmake .. && $bear make -j8"
    }
}

fun Path.assertAllFilesNotEmptyRecursively() {
    val visitor = object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
            if (attrs?.isRegularFile == true && file?.readText()?.isEmpty() == true) {
                throw AssertionError("Found an empty file: $file")
            }
            return FileVisitResult.CONTINUE
        }
    }
    Files.walkFileTree(this, visitor)
}

fun Path.assertFileOrDirExists(message: String = "") {
    assert(this.exists()) { "$this does not exist!\n${message}" }
}
