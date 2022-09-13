#include <utils/SanitizerUtils.h>
#include <utils/ArgumentsUtils.h>
#include "CMakeListsPrinter.h"
#include "utils/Copyright.h"
#include "utils/LinkerUtils.h"
#include "utils/FileSystemUtils.h"
#include "building/Linker.h"
#include "Synchronizer.h"
#include "loguru.h"

namespace printer {
    CMakeListsPrinter::CMakeListsPrinter(const BaseTestGen *testGen) :
            CMakeListsPrinter(testGen, CompilationUtils::getBundledCompilerPath(CompilationUtils::getCompilerName(
                    testGen->getTargetBuildDatabase()->compilationDatabase->getBuildCompilerPath()))) {}

    CMakeListsPrinter::CMakeListsPrinter(const BaseTestGen *testGen, fs::path primaryCompiler)
            : testGen(testGen),
              primaryCompiler(std::move(primaryCompiler)),
              primaryCxxCompiler(CompilationUtils::toCppCompiler(this->primaryCompiler)),
              primaryCompilerName(CompilationUtils::getCompilerName(this->primaryCompiler)),
              primaryCxxCompilerName(CompilationUtils::getCompilerName(this->primaryCxxCompiler)),
              coverageLinkFlags(
                      StringUtils::joinWith(CompilationUtils::getCoverageLinkFlags(this->primaryCxxCompilerName), ";")),
              sanitizerLinkFlags(SanitizerUtils::getSanitizeLinkFlags(this->primaryCxxCompilerName)),
              serverTestsDir(testGen->projectContext.testDirPath),
              currentCMakeDir(serverTestsDir) {}

    void CMakeListsPrinter::generate(const Linker::LinkResult &linkResult) {
        generate(Paths::removeExtension(linkResult.bitcodeOutput), linkResult.stubsSet, linkResult.presentedFiles);
    }

    void CMakeListsPrinter::generate(const fs::path &target, const CollectionUtils::FileSet &stubsSet,
                                     const CollectionUtils::FileSet &presentedFiles) {
        generateCMakeLists(presentedFiles);
        generateCMakeForTargetRecursively(target, stubsSet);
        addTests(presentedFiles, target, stubsSet);
    }

    void CMakeListsPrinter::addVariable(const std::string &varName, const std::string &value) {
        ss << "set(" << varName << " " << value << ")\n";
    }

    void CMakeListsPrinter::generateCMakeLists(const CollectionUtils::FileSet &testsSourcePaths) {
        auto printer = CMakeListsPrinter(testGen);
        printer.addVariable(CMakeListsPrinter::SANITIZER_FLAGS_VAR_NAME,
                            StringUtils::joinWith(SANITIZER_NEEDED_FLAGS, ";"));
        printer.addVariable(CMakeListsPrinter::COVERAGE_LINK_FLAGS_VAR_NAME, coverageLinkFlags);
        printer.addVariable(CMakeListsPrinter::SANITIZER_LINK_FLAGS_VAR_NAME, sanitizerLinkFlags);
        for (auto &sourceFile: testsSourcePaths) {
            printer.addInclude(
                    getCMakeFileForTestFile(Paths::sourcePathToTestPath(testGen->projectContext, sourceFile)));
        }
        printer.write(testGen->projectContext.testDirPath / "CMakeLists.txt");
    }

    void CMakeListsPrinter::generateCMakeForTargetRecursively(const fs::path &target,
                                                              const CollectionUtils::FileSet &stubsSet) {
        auto printer = CMakeListsPrinter(testGen, primaryCompiler);
        printer.addLinkTargetRecursively(target, true, stubsSet);
    }

    fs::path CMakeListsPrinter::getTargetCmakePath(const fs::path &lib) {
        return testGen->projectContext.testDirPath / (getLibraryName(lib, true) + ".cmake");
    }

    void CMakeListsPrinter::addLinkTargetRecursively(const fs::path &path, bool isRoot,
                                                     const CollectionUtils::FileSet &stubsSet) {
        if (CollectionUtils::contains(alreadyBuildFiles, path)) {
            return;
        }
        auto targetBuildDb = testGen->getTargetBuildDatabase();
        auto targetInfo = targetBuildDb->getClientLinkUnitInfo(path);
        std::vector<std::string> dependentLibs;
        CMakeListsPrinter::FileToObjectInfo dependentSourceFiles;
        for (auto &file: targetInfo->files) {
            if (Paths::isObjectFile(file)) {
                auto objectInfo = testGen->getClientCompilationUnitInfo(file);
                auto fileToAdd = objectInfo->getSourcePath();
                if (Paths::isCFile(fileToAdd)) {
                    fileToAdd = Paths::getWrapperFilePath(testGen->projectContext, fileToAdd);
                }
                dependentSourceFiles.emplace(fileToAdd, objectInfo);
            } else {
                dependentLibs.push_back(getLibraryName(file, false));
            }
            alreadyBuildFiles.insert(file);
        }
        auto isExecutable = !Paths::isLibraryFile(path);
        auto isLinkAsShared = isRoot || isExecutable || Paths::isSharedLibraryFile(path);
        if (isLinkAsShared) {
            // link with stubs
            for (auto &stubFile: stubsSet) {
                auto objectInfo = testGen->getClientCompilationUnitInfo(
                        Paths::stubPathToSourcePath(testGen->projectContext, stubFile), true);
                dependentSourceFiles.emplace(stubFile, objectInfo);
            }
        }
        auto libName = getLibraryName(path, isRoot);
        addLibrary(libName, isLinkAsShared, dependentSourceFiles);
        addOptionsForSourceFiles(dependentSourceFiles);
        if (!dependentLibs.empty()) {
            for (auto &lib: dependentLibs) {
                auto printer = CMakeListsPrinter(testGen, primaryCompiler);
                printer.addLinkTargetRecursively(lib, false, stubsSet);
                auto libFile = testGen->projectContext.testDirPath.parent_path() / getLibraryName(lib, false);
                addInclude(libFile);
            }
            addTargetLinkLibraries(libName, dependentLibs);
        }
        addLinkFlagsForLibrary(libName, path, isLinkAsShared);
        write(getTargetCmakePath(path));
    }

    void CMakeListsPrinter::addInclude(const fs::path &cmakeFileToInclude) {
        ss << "include" << "(" << fs::relative(cmakeFileToInclude, serverTestsDir).string()
           << ")\n";
    }

    void CMakeListsPrinter::addOptionsForSourceFiles(const CMakeListsPrinter::FileToObjectInfo &sourceFiles) {
        std::list<std::string> commonFlags {
                // StringUtils::joinWith(SanitizerUtils::getSanitizeCompileFlags(primaryCompilerName), ";"),
                // StringUtils::joinWith(CompilationUtils::getCoverageCompileFlags(primaryCompilerName), ";"),
                // OPTIMIZATION_FLAG, FPIC_FLAG,
                // wrapCMakeVariable(CMakeListsPrinter::SANITIZER_FLAGS_VAR_NAME)
        };
        auto varName = "COMMON_FLAGS";
        addVariable(varName, StringUtils::joinWith(commonFlags, ";"));
        for (auto &[file, compilationInfo]: sourceFiles) {
            utbot::CompileCommand compileCommand = compilationInfo->command;
            // compiler will be set by cmake
            compileCommand.removeBuildTool();
            compileCommand.removeCompileOption();
            compileCommand.removeOutput();
            compileCommand.addFlagToBegin(wrapCMakeVariable(varName));
            for (auto &argument: compileCommand.getCommandLine()) {
                tryChangeToAbsolute(argument);
            }
            compileCommand.addFlagToBegin(
                    StringUtils::stringFormat("-iquote %s", getAbsolutePath(
                            compilationInfo->getSourcePath().parent_path())));

            auto options = StringUtils::joinWith(compileCommand.getCommandLine(), ";");
            setCompileOptionsForSource(file, options);
        }
    }

    void CMakeListsPrinter::setCompileOptionsForSource(const fs::path &sourceFile, const std::string &options) {
        ss << "set_source_files_properties(" << getRelativePath(sourceFile).string() << " PROPERTIES COMPILE_OPTIONS "
           << '"' <<
           options << '"' << ")" << NL;
    }

    std::string CMakeListsPrinter::wrapCMakeVariable(const std::string &variableName) {
        return "${" + variableName + "}";
    }

    void CMakeListsPrinter::tryChangeToAbsolute(std::string &argument) {
        LOG_S(INFO) << "argument before conversion: " << argument;
        if (StringUtils::startsWith(argument, "/")) {
            argument = getAbsolutePath(argument);
        } else if (argument.length() >= 3 && StringUtils::startsWith(argument, "-I")) { // if in -I flag
            argument = CompilationUtils::getIncludePath(fs::path(getAbsolutePath(argument.substr(2)), false));
        }
        LOG_S(INFO) << "argument after conversion " << argument;
    }

    CMakeListsPrinter::std_path CMakeListsPrinter::getAbsolutePath(const fs::path &path) {
        auto relativeFromProjectToPath = fs::relative(path, testGen->projectContext.projectPath);
        auto relativeFromTestsToProject = fs::relative(testGen->projectContext.projectPath,
                                                       testGen->projectContext.testDirPath);
        LOG_S(INFO) << relativeFromProjectToPath << " " << relativeFromTestsToProject;
        auto res = fs::path("${CMAKE_CURRENT_SOURCE_DIR}").resolve(relativeFromTestsToProject, false).resolve(
                relativeFromProjectToPath, false);
        LOG_S(INFO) << "Result of " << path << " to " << res;
        return res.to_std();
    }

    fs::path CMakeListsPrinter::getRelativePath(const fs::path &path) {
        return fs::relative(path, serverTestsDir, false);
    }

    std::string CMakeListsPrinter::getLibraryName(const fs::path &lib, bool isRoot) {
        return lib.stem().string() +
               (isRoot && Paths::isStaticLibraryFile(lib) ? "_shared" : "") + "_utbot";
    }

    fs::path CMakeListsPrinter::getCMakeFileForTestFile(const fs::path &testFile) {
        return Paths::replaceExtension(testFile, "cmake");
    }

    void CMakeListsPrinter::addLibrary(const std::string &libraryName, bool isShared,
                                       const CMakeListsPrinter::FileToObjectInfo &sourceFiles) {
        ss << "add_library" << LBr();
        ss << LINE_INDENT() << libraryName << NL;
        if (isShared)
            ss << LINE_INDENT() << "SHARED" << NL;
        for (auto &[file, _]: sourceFiles) {
            ss << LINE_INDENT() << getRelativePath(file).string() << NL;
        }
        ss << RBr();
    }

    void CMakeListsPrinter::addTests(const CollectionUtils::FileSet &filesUnderTest, const fs::path &target,
                                     const CollectionUtils::FileSet &stubSet) {
        for (auto &sourceFile: filesUnderTest) {
            auto printer = CMakeListsPrinter(testGen, primaryCompiler);
            printer.addTestExecutable(sourceFile, stubSet, target);
            printer.write(getCMakeFileForTestFile(Paths::sourcePathToTestPath(testGen->projectContext, sourceFile)));
        }
    }

    void CMakeListsPrinter::addTestExecutable(const fs::path &path, const CollectionUtils::FileSet &stubs,
                                              const fs::path &target) {
        auto testPath = Paths::sourcePathToTestPath(testGen->projectContext, path);
        auto testName = getTestName(testPath);

        addExecutable(testName, {testPath});
        setCompileOptionsForSource(testPath, StringUtils::joinWith(prepareCompileFlagsForTestFile(path), ";"));
        addInclude(getTargetCmakePath(target));
        addTargetLinkLibraries(testName, {"GTest::gtest_main", getRootLibraryName(target)});
        addLinkOptionsForTestTarget(testName, target);
        addDiscoverTestDirective(testName);
    }

    std::list<std::string> CMakeListsPrinter::prepareCompileFlagsForTestFile(const fs::path &sourcePath) {
        auto testPath = Paths::sourcePathToTestPath(testGen->projectContext, sourcePath);
        auto compilationUnitInfo = testGen->getClientCompilationUnitInfo(sourcePath, true);
        auto testCompilationCommand = compilationUnitInfo->command;
        testCompilationCommand.removeBuildTool();
        testCompilationCommand.removeCompileOption();
        testCompilationCommand.removeOutput();
        testCompilationCommand.setOptimizationLevel(OPTIMIZATION_FLAG);
        testCompilationCommand.removeCompilerFlagsAndOptions(UNSUPPORTED_FLAGS_AND_OPTIONS_TEST_MAKE);
        testCompilationCommand.removeIncludeFlags();
        testCompilationCommand.addFlagToBegin(FPIC_FLAG);
        testCompilationCommand.addFlagsToBegin(SANITIZER_NEEDED_FLAGS);
        return testCompilationCommand.getCommandLine();
    }

    std::string CMakeListsPrinter::getRootLibraryName(const fs::path &path) {
        return getLibraryName(path, true);
    }

    void CMakeListsPrinter::addExecutable(const std::string &executableName, const std::vector<fs::path> &sourceFiles) {
        ss << "add_executable" << LBr() << LINE_INDENT() << executableName << NL;
        for (auto &file: sourceFiles) {
            ss << LINE_INDENT() << getRelativePath(file).string() << NL;
        }
        ss << RBr();
    }

    void CMakeListsPrinter::addLinkOptionsForTestTarget(const std::string &testName, const fs::path &target) {
        auto rootLinkUnitInfo = testGen->getTargetBuildDatabase()->getClientLinkUnitInfo(target);
        auto linkCommand = rootLinkUnitInfo->commands.front();
        std::vector<std::string> dynamicLinkCommandLine{wrapCMakeVariable(COVERAGE_LINK_FLAGS_VAR_NAME),
                                                        wrapCMakeVariable(SANITIZER_FLAGS_VAR_NAME)};
        if (!linkCommand.isArchiveCommand())
            dynamicLinkCommandLine.insert(dynamicLinkCommandLine.begin(), OPTIMIZATION_FLAG);
        setLinkOptionsForTarget(testName, StringUtils::joinWith(dynamicLinkCommandLine, ";"));
    }

    void CMakeListsPrinter::setLinkOptionsForTarget(const std::string &targetName, const std::string &options) {
        ss << NL << "target_link_options(" << targetName << " PUBLIC " << options << ")" << NL;
    }


    std::string CMakeListsPrinter::getTestName(const fs::path &test) {
        return test.stem().string();
    }

    void CMakeListsPrinter::addTargetLinkLibraries(const std::string &targetName,
                                                   const std::vector<std::string> &librariesNamesToLink) {
        ss << "target_link_libraries" << LBr() << LINE_INDENT() << targetName << NL;
        for (auto &path: librariesNamesToLink) {
            ss << LINE_INDENT() << path << NL;
        }
        ss << RBr();
    }

    void CMakeListsPrinter::addDiscoverTestDirective(const std::string &testTargetName) {
        ss << "gtest_discover_tests" << LBr() << LINE_INDENT() << testTargetName << RBr();
    }

    void CMakeListsPrinter::addLinkFlagsForLibrary(const std::string &targetName, const fs::path &targetPath,
                                                   bool transformExeToLib) {
        auto linkUnitInfo = testGen->getProjectBuildDatabase()->getClientLinkUnitInfo(targetPath);

        // assuming there is only one link command
        auto command = linkUnitInfo->commands[0];
        if (linkUnitInfo->commands.size() > 1) {
            LOG_S(WARNING) << "There are multiple link commands for " << linkUnitInfo->getOutput() << NL;
        }
        setLinkOptionsForTarget(targetName,
                                StringUtils::joinWith(prepareFlagsForLinkCommand(command, transformExeToLib), ";"));
    }

    std::list<std::string>
    CMakeListsPrinter::prepareFlagsForLinkCommand(utbot::LinkCommand &linkCommand, bool transformExeToLib) {
        auto output = linkCommand.getOutput();
        auto isExecutable = Paths::isLibraryFile(output);
        std::list<std::string> commandLine = {};
        if (!linkCommand.isArchiveCommand()) {
            if (!isExecutable || transformExeToLib) {
                commandLine.insert(commandLine.begin(), {"-Wl,--allow-multiple-definition",
                                                         wrapCMakeVariable(COVERAGE_LINK_FLAGS_VAR_NAME),
                                                         wrapCMakeVariable(SANITIZER_FLAGS_VAR_NAME),
                });
                commandLine.insert(commandLine.begin(), OPTIMIZATION_FLAG);
            }
            commandLine.insert(commandLine.begin(), "-fuse-ld=gold");
            if (isExecutable) {
                commandLine.insert(commandLine.begin(), transformExeToLib ? SHARED_FLAG : RELOCATE_FLAG);
            }
        }
        /**
        // todo: pthread flag
        // todo: access private lib?
        linkCommand.erase(STATIC_FLAG);
        auto output = linkCommand.getOutput();
        auto isExecutable = Paths::isLibraryFile(output);
        linkCommand.removeCompileOption();
        linkCommand.removeBuildTool();
        linkCommand.removeOutput();
        for (auto &arg: linkCommand.getCommandLine()) {
            tryChangeToAbsolute(arg);
        }
        if (!linkCommand.isArchiveCommand()) {
            if (isExecutable && !transformExeToLib) {
                for (std::string &argument: linkCommand.getCommandLine()) {
                    LinkerUtils::transformCompilerFlagsToLinkerFlags(argument);
                }
            }
            for (std::string &argument: linkCommand.getCommandLine()) {
                LinkerUtils::removeScriptFlag(argument);
                LinkerUtils::removeSonameFlag(argument);
            }
            if (!isExecutable || transformExeToLib) {
                linkCommand.addFlagsToBegin({"-Wl,--allow-multiple-definition",
                                             coverageLinkFlags, sanitizerLinkFlags, "-Wl,--whole-archive"});
                linkCommand.addFlagToEnd("-Wl,--no-whole-archive");
                linkCommand.setOptimizationLevel(OPTIMIZATION_FLAG);
            }
            if (isExecutable) {
                linkCommand.addFlagToBegin(transformExeToLib ? SHARED_FLAG : RELOCATE_FLAG);
            }
        } else {

        }
            */
        return commandLine;
    }

    void CMakeListsPrinter::write(const fs::path &path) {
        FileSystemUtils::writeToFile(path, ss.str());
    }

    void CMakeListsPrinter::writeCopyrightHeader() {
        ss << Copyright::GENERATED_CMAKELISTS_FILE_HEADER << NL;
    }

    std::string CMakeListsPrinter::RBr() {
        tabsDepth--;
        return LINE_INDENT() + ")\n";
    }

    std::string CMakeListsPrinter::LBr(bool startsWithSpace) {
        tabsDepth++;
        return std::string(startsWithSpace ? " " : "") + "(\n";
    }
}
