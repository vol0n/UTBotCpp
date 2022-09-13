#include "LinkerUtils.h"

#include "Paths.h"
#include "StringUtils.h"
#include "exceptions/UnImplementedException.h"

namespace LinkerUtils {
    static inline const std::string STUB_SUFFIX = "_stub";

    fs::path applySuffix(const fs::path &output,
                         BuildResult::Type unitType,
                         const std::string &suffixForParentOfStubs) {
        switch (unitType) {
        case BuildResult::Type::ALL_STUBS:
            return Paths::addSuffix(output, STUB_SUFFIX);
        case BuildResult::Type::ANY_STUBS:
            return Paths::addSuffix(output, suffixForParentOfStubs);
        case BuildResult::Type::NO_STUBS:
            return output;
        case BuildResult::Type::NONE:
            throw UnImplementedException(StringUtils::stringFormat(
                "Applying suffix for file %s which has invalid type", output));
        }
    }

    void eraseIfWlOnly(std::string &argument) {
        if (argument == "-Wl") {
            argument = "";
        }
    }

    void removeLinkerFlag(std::string &argument, std::string const &flag) {
        auto options = StringUtils::split(argument, ',');
        size_t erased = CollectionUtils::erase_if(options, [&flag](std::string const &option) {
            return StringUtils::startsWith(option, flag);
        });
        if (erased == 0) {
            return;
        }
        argument = StringUtils::joinWith(options, ",");
        eraseIfWlOnly(argument);
    }

    // transforms -Wl,<arg>,<arg2>... to <arg> <arg2>...
    // https://clang.llvm.org/docs/ClangCommandLineReference.html#cmdoption-clang-wl-arg-arg2
    void transformCompilerFlagsToLinkerFlags(std::string &argument) {
        auto options = StringUtils::split(argument, ',');
        if (options.empty()) {
            return;
        }
        if (options.front() != "-Wl") {
            return;
        }
        CollectionUtils::erase(options, options.front());
        argument = StringUtils::joinWith(options, " ");
    }

    void removeScriptFlag(std::string &argument) {
        removeLinkerFlag(argument, "--version-script");
    }

    void removeSonameFlag(std::string &argument) {
        auto options = StringUtils::split(argument, ',');
        bool isSonameNext = false;
        std::vector<std::string> result;
        for (std::string const &option : options) {
            if (option == "-soname") {
                isSonameNext = true;
                continue;
            }
            if (isSonameNext) {
                isSonameNext = false;
                continue;
            }
            result.push_back(option);
        }
        argument = StringUtils::joinWith(result, ",");
        eraseIfWlOnly(argument);
    }
}
