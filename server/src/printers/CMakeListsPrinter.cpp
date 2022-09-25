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
    void CMakeListsPrinter::addVariable(const std::string &varName, const std::string &value) {
        ss << "set(" << varName << " " << value << ")\n";
    }


    std::string CMakeListsPrinter::wrapCMakeVariable(const std::string &variableName) {
        return "${" + variableName + "}";
    }

    void CMakeListsPrinter::setLinkOptionsForTarget(const std::string &targetName, const std::string &options) {
        ss << NL << "target_link_options(" << targetName << " PUBLIC " << options << ")" << NL;
    }


    void CMakeListsPrinter::addDiscoverTestDirective(const std::string &testTargetName) {
        ss << "gtest_discover_tests(" << testTargetName << ")" << NL;
    }

    void CMakeListsPrinter::addLibrary(const std::string &libraryName, bool isShared, const std::vector<fs::path> &sourceFiles) {
        ss << "add_library" << LBr();
        ss << LINE_INDENT() << libraryName << NL;
        if (isShared)
            ss << LINE_INDENT() << "SHARED" << NL;
        for (auto &file : sourceFiles) {
            ss << LINE_INDENT() << getRelativePath(file).string() << NL;
        }
        ss << RBr();
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

    void CMakeListsPrinter::addIncludeDirectoriesForTarget(const std::string &targetName,
                                                           const std::set<std::string> includePaths) {
        ss << "target_include_directories" << LBr() << targetName << NL;
        for (auto &includePath : includePaths) {
            ss << LINE_INDENT() << includePath << NL;
        }
        ss << RBr();
    }

    void CMakeListsPrinter::addTargetLinkLibraries(const std::string &targetName,
                                                   const std::vector<std::string> &librariesNamesToLink) {
        ss << "target_link_libraries" << LBr() << targetName << NL;
        for (auto &lib : librariesNamesToLink) {
            ss << LINE_INDENT() << lib << NL;
        }
        ss << RBr();
    }

    CMakeListsPrinter::CMakeListsPrinter() {}
}
