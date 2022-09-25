//
// Created by Арсений Волынец on 13.09.2022.
//

#ifndef UTBOTCPP_CMAKEGENERATOR_H
#define UTBOTCPP_CMAKEGENERATOR_H

#include "testgens/BaseTestGen.h"
#include "BuildResult.h"
#include "printers/CMakeListsPrinter.h"

#include <string>
#include <vector>
#include <building/Linker.h>

class CMakeGenerator {
public:
    CMakeGenerator() = delete;
    CMakeGenerator(const CMakeGenerator &other) = delete;
    explicit CMakeGenerator(const BaseTestGen* testGen, fs::path primaryCompiler);

    std::string SANITIZER_FLAGS_VAR_NAME="SANITIZER_FLAGS";
    std::string SANITIZER_LINK_FLAGS_VAR_NAME="SANITIZER_LINK_FLAGS";
    std::string COVERAGE_LINK_FLAGS_VAR_NAME="COVERAGE_LINK_FLAGS";
    using std_path = std::filesystem::path;

    static inline std::string GTEST_TARGET_NAME = "GTest::gtest_main";

    CollectionUtils::FileSet alreadyBuildFiles;
    printer::CMakeListsPrinter printer;
    const fs::path primaryCompiler;
    const fs::path primaryCxxCompiler;
    const fs::path serverTestsDir;
    const fs::path currentCMakeDir;


    using FileToObjectInfo = CollectionUtils::MapFileTo<std::shared_ptr<const BuildDatabase::ObjectFileInfo>>;

    void generate(const Linker::LinkResult &linkResult);
    void generate(const fs::path &target, const CollectionUtils::FileSet &stubsSet,
                  const CollectionUtils::FileSet &presentedFiles);

    void addIncludeDirectoriesForTarget(const fs::path &target, bool isRoot);

    void addLinkTargetRecursively(const fs::path &path, bool isRoot, const CollectionUtils::FileSet &stubsSet);

    void addTests(const CollectionUtils::FileSet &filesUnderTest, const fs::path &target,
             const CollectionUtils::FileSet &stubSet);

    void generateCMakeForTargetRecursively(const fs::path &target, const CollectionUtils::FileSet& stubsSet);

    void generateCMakeLists(const CollectionUtils::FileSet &testsSourcePaths);

    std_path getAbsolutePath(const fs::path &path);

    std::set<std::string> getIncludeDirectoriesFor(const fs::path &target);

    std::string getLibraryName(const fs::path &lib, bool isRoot);

    fs::path getRelativePath(const fs::path &path);

    std::string getRootLibraryName(const fs::path &path);

    std::shared_ptr<const BuildDatabase::TargetInfo> getTargetUnitInfo(const fs::path &targetPath);

    const BaseTestGen *testGen;

private:
    fs::path getTargetCmakePath(const fs::path &lib);

    void addOptionsForSourceFiles(const FileToObjectInfo &sourceFiles);

    void addLinkFlagsForLibrary(const std::string &targetName, const fs::path &targetPath, bool transformExeToLib = false);
    std::string getTestName(const fs::path &test);

    void addLinkOptionsForTestTarget(const std::string &testName, const fs::path &target);

    void tryChangeToAbsolute(std::string &argument);

    void setCompileOptionsForSource(const fs::path &sourceFile, const std::string &options);

    std::list<std::string> prepareCompileFlagsForTestFile(const fs::path &sourcePath);

    std::list<std::string> prepareFlagsForLinkCommand(utbot::LinkCommand &linkCommand, bool transformExeToLib);
};

#endif //UTBOTCPP_CMAKEGENERATOR_H
