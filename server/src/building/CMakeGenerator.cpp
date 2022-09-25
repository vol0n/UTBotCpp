//
// Created by Арсений Волынец on 13.09.2022.
//

#include "loguru.h"
#include "Synchronizer.h"
#include "building/Linker.h"
#include "utils/FileSystemUtils.h"
#include "utils/LinkerUtils.h"
#include "utils/Copyright.h"
#include <utils/ArgumentsUtils.h>
#include <utils/SanitizerUtils.h>
#include "CMakeGenerator.h"

void CMakeGenerator::generate(const Linker::LinkResult &linkResult) {
    generate(Paths::removeExtension(linkResult.bitcodeOutput), linkResult.stubsSet, linkResult.presentedFiles);
}

void CMakeGenerator::generate(const fs::path &target, const CollectionUtils::FileSet &stubsSet,
                              const CollectionUtils::FileSet &presentedFiles) {
    generateCMakeForTargetRecursively(target, stubsSet);
    addTests(presentedFiles, target, stubsSet);
    printer.write(testGen->projectContext.testDirPath / "CMakeLists.txt");
}

void CMakeGenerator::generateCMakeForTargetRecursively(const fs::path &target,
                                                       const CollectionUtils::FileSet &stubsSet) {
    addLinkTargetRecursively(target, true, stubsSet);
}

fs::path CMakeGenerator::getTargetCmakePath(const fs::path &lib) {
    return testGen->projectContext.testDirPath / (getLibraryName(lib, true) + ".cmake");
}

std::shared_ptr<const BuildDatabase::TargetInfo> CMakeGenerator::getTargetUnitInfo(const fs::path &targetPath) {
    auto targetBuildDb = testGen->getTargetBuildDatabase();
    return targetBuildDb->getClientLinkUnitInfo(targetPath);
}

void CMakeGenerator::addLinkTargetRecursively(const fs::path &path, bool isRoot,
                                              const CollectionUtils::FileSet &stubsSet) {
    if (CollectionUtils::contains(alreadyBuildFiles, path)) {
        return;
    }
    auto targetInfo = getTargetUnitInfo(path);
    std::vector<std::string> dependentLibs;
    std::vector<fs::path> dependentSourceFiles;
    for (auto &file: targetInfo->files) {
        if (Paths::isObjectFile(file)) {
            auto objectInfo = testGen->getClientCompilationUnitInfo(file);
            auto fileToAdd = objectInfo->getSourcePath();
            if (Paths::isCFile(fileToAdd)) {
                fileToAdd = Paths::getWrapperFilePath(testGen->projectContext, fileToAdd);
            }
            dependentSourceFiles.push_back(fileToAdd);
        } else {
            dependentLibs.push_back(getLibraryName(file, false));
        }
        alreadyBuildFiles.insert(file);
    }
    auto isExecutable = !Paths::isLibraryFile(path);
    auto linkWithStubs = isRoot || isExecutable || Paths::isSharedLibraryFile(path);
    if (linkWithStubs) {
        for (auto &stubFile: stubsSet) {
            dependentSourceFiles.push_back(stubFile);
        }
    }
    auto libName = getLibraryName(path, isRoot);
    printer.addLibrary(libName, linkWithStubs, dependentSourceFiles);
    printer.addIncludeDirectoriesForTarget(libName, getIncludeDirectoriesFor(path));
    if (!dependentLibs.empty()) {
        for (auto &lib: dependentLibs) {
            addLinkTargetRecursively(lib, false, stubsSet);
        }
        printer.addTargetLinkLibraries(libName, dependentLibs);
    }
}

std::set<std::string> CMakeGenerator::getIncludeDirectoriesFor(const fs::path &target) {
    auto targetInfo = getTargetUnitInfo(target);
    std::set<std::string> res;
    for (auto &file: targetInfo->files) {
        if (!Paths::isObjectFile(file))
            continue;
        auto objectInfo = testGen->getClientCompilationUnitInfo(file);
        for (auto &arg: objectInfo->command.getCommandLine()) {
            if (StringUtils::startsWith(arg, "-I")) {
                res.insert(getAbsolutePath(arg.substr(2)).string());
            }
        }
    }
    return res;
}

CMakeGenerator::std_path CMakeGenerator::getAbsolutePath(const fs::path &path) {
    auto relativeFromProjectToPath = fs::relative(path, testGen->projectContext.projectPath);
    auto relativeFromTestsToProject = fs::relative(testGen->projectContext.projectPath,
                                                   testGen->projectContext.testDirPath);
    auto res = fs::path("${CMAKE_CURRENT_SOURCE_DIR}").resolve(relativeFromTestsToProject, false).resolve(
            relativeFromProjectToPath, false);
    return res.to_std();
}

std::string CMakeGenerator::getLibraryName(const fs::path &lib, bool isRoot) {
    return lib.stem().string() +
           (isRoot && Paths::isStaticLibraryFile(lib) ? "_shared" : "") + "_utbot";
}

void CMakeGenerator::addTests(const CollectionUtils::FileSet &filesUnderTest, const fs::path &target,
                              const CollectionUtils::FileSet &stubSet) {
    std::vector<fs::path> testFiles;
    for (auto &file: filesUnderTest) {
        testFiles.push_back(Paths::sourcePathToTestPath(testGen->projectContext, file));
    }
    auto testsTargetName = "utbot_tests";
    printer.addExecutable(testsTargetName, CollectionUtils::transformTo<std::vector<fs::path>>(filesUnderTest,
                                                                               [&](const fs::path &elem) {
                                                                                   return fs::relative(
                                                                                           elem,
                                                                                           testGen->projectContext.testDirPath);
                                                                               }));
    printer.addTargetLinkLibraries(testsTargetName, {"GTest::gtest_main", getRootLibraryName(target)});
    printer.addDiscoverTestDirective(testsTargetName);
}

std::string CMakeGenerator::getRootLibraryName(const fs::path &path) {
    return getLibraryName(path, true);
}
