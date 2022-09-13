#ifndef UNITTESTBOT_LINKERUTILS_H
#define UNITTESTBOT_LINKERUTILS_H

#include "BuildResult.h"

#include "utils/path/FileSystemPath.h"
#include <string>

namespace LinkerUtils {
    fs::path applySuffix(const fs::path &output,
                         BuildResult::Type unitType,
                         const std::string &suffixForParentOfStubs);

    void eraseIfWlOnly(std::string &argument);

    void removeLinkerFlag(std::string &argument, std::string const &flag);

    // transforms -Wl,<arg>,<arg2>... to <arg> <arg2>...
    // https://clang.llvm.org/docs/ClangCommandLineReference.html#cmdoption-clang-wl-arg-arg2
    void transformCompilerFlagsToLinkerFlags(std::string &argument);

    void removeScriptFlag(std::string &argument);

    void removeSonameFlag(std::string &argument);
};

#endif // UNITTESTBOT_LINKERUTILS_H
