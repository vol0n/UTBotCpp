#ifndef UTBOTCPP_CMAKELISTSPRINTER_H
#define UTBOTCPP_CMAKELISTSPRINTER_H

#include "Printer.h"
#include "testgens/BaseTestGen.h"
#include "BuildResult.h"

#include <string>
#include <vector>
#include <building/Linker.h>


namespace printer {
    class CMakeListsPrinter {
    public:
        CMakeListsPrinter() = delete;
        CMakeListsPrinter(const CMakeListsPrinter &other) = delete;
        explicit CMakeListsPrinter(const BaseTestGen* testGen, fs::path primaryCompiler);

        std::string SANITIZER_FLAGS_VAR_NAME="SANITIZER_FLAGS";
        std::string SANITIZER_LINK_FLAGS_VAR_NAME="SANITIZER_LINK_FLAGS";
        std::string COVERAGE_LINK_FLAGS_VAR_NAME="COVERAGE_LINK_FLAGS";
        using std_path = std::filesystem::path;


        CMakeListsPrinter(const BaseTestGen *testGen);

        std::string LBr(bool startsWithSpace = false);

        std::string RBr();

        static inline std::string GTEST_TARGET_NAME = "GTest::gtest_main";

        std::stringstream ss;
        int tabsDepth = 0;
        CollectionUtils::FileSet alreadyBuildFiles;
        const fs::path primaryCompiler;
        const fs::path primaryCxxCompiler;
        const CompilationUtils::CompilerName primaryCompilerName;
        const CompilationUtils::CompilerName primaryCxxCompilerName;
        const std::string coverageLinkFlags;
        const std::string sanitizerLinkFlags;
        const fs::path serverTestsDir;
        const fs::path currentCMakeDir;

        using FileToObjectInfo = CollectionUtils::MapFileTo<std::shared_ptr<const BuildDatabase::ObjectFileInfo>>;

        inline std::string LINE_INDENT() const {
            return StringUtils::repeat(TAB, tabsDepth);
        }

        void addTargetLinkLibraries(const std::string &targetName, const std::vector<std::string>& librariesNamesToLink);
        void generate(const Linker::LinkResult &linkResult);
        void generate(const fs::path &target, const CollectionUtils::FileSet &stubsSet,
                      const CollectionUtils::FileSet &presentedFiles);

    protected:
        void writeCopyrightHeader();
    private:
        const BaseTestGen *testGen;
    private:
        void addDiscoverTestDirective(const std::string &testTargetName);
        void generateCMakeForTargetRecursively(const fs::path &target, const CollectionUtils::FileSet& stubsSet);
        void addLinkTargetRecursively(const fs::path &path, bool isRoot, const CollectionUtils::FileSet &stubsSet);
        void write(const fs::path &path);
        void
        addTests(const CollectionUtils::FileSet &filesUnderTest, const fs::path &target,
                 const CollectionUtils::FileSet &stubSet);
        fs::path getTargetCmakePath(const fs::path &lib);
        void addInclude(const fs::path &cmakeFileToInclude);
        std_path getAbsolutePath(const fs::path &path);
        fs::path getRelativePath(const fs::path &path);
        void addOptionsForSourceFiles(const FileToObjectInfo &sourceFiles);
        void addLibrary(const std::string &libraryName, bool isShared, const FileToObjectInfo &sourceFiles);
        void addLinkFlagsForLibrary(const std::string &targetName, const fs::path &targetPath, bool transformExeToLib = false);
        fs::path getCMakeFileForTestFile(const fs::path &testFile);
        void generateCMakeLists(const CollectionUtils::FileSet &testsSourcePaths);
        std::list<std::string>
        prepareFlagsForLinkCommand(utbot::LinkCommand &LinkCommand, bool transformExeToLib);

        std::string getTestName(const fs::path &test);

        void addTestExecutable(const fs::path &path, const CollectionUtils::FileSet &stubs, const fs::path &target);

        void addLinkOptionsForTestTarget(const std::string &testName, const fs::path &target);

        void addVariable(const std::string &varName, const std::string &value);

        std::string wrapCMakeVariable(const std::string &variableName);

        void tryChangeToAbsolute(std::string &argument);


        void setCompileOptionsForSource(const fs::path &sourceFile, const std::string &options);

        void addExecutable(const std::string &executableName, const std::vector<fs::path> &sourceFiles);

        std::string getRootLibraryName(const fs::path &path);

        std::string getLibraryName(const fs::path &lib, bool isRoot);

        std::list<std::string> prepareCompileFlagsForTestFile(const fs::path &sourcePath);

        void setLinkOptionsForTarget(const std::string &targetName, const std::string &options);
    };
}

#endif //UTBOTCPP_CMAKELISTSPRINTER_H
