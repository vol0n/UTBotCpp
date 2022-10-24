#include "CompilationDatabase.h"

#include "Paths.h"
#include "exceptions/CompilationDatabaseException.h"
#include "utils/CompilationUtils.h"

CompilationDatabase::CompilationDatabase(
    std::unique_ptr<clang::tooling::CompilationDatabase> clangCompilationDatabase_)
    : clangCompilationDatabase(std::move(clangCompilationDatabase_)) {
    LOG_S(INFO) << "in constructor of CompilationDatabase";
    allFiles = initAllFiles();
    LOG_S(INFO) << "after initAllFiles";
    buildCompilerPath = initBuildCompilerPath();
    LOG_S(INFO) << "after init build compiler path";
    resourceDir = CompilationUtils::getResourceDirectory(buildCompilerPath);
    LOG_S(INFO) << "after getting resource dir";
}

CollectionUtils::FileSet CompilationDatabase::initAllFiles() const {
    LOG_S(INFO) << "calling get all files from clang cdb";
    auto files = clangCompilationDatabase->getAllFiles();
    LOG_S(INFO) << "after getting all files of clang cdb";
    return CollectionUtils::transformTo<CollectionUtils::FileSet>(
        files, [](fs::path const &path) { return fs::weakly_canonical(path); });
}

fs::path CompilationDatabase::initBuildCompilerPath() {
    for (auto const &compileCommand : clangCompilationDatabase->getAllCompileCommands()) {
        fs::path compilerPath = fs::weakly_canonical(compileCommand.CommandLine[0]);
        auto compilerName = CompilationUtils::getCompilerName(compilerPath);
        if (compilerName != CompilationUtils::CompilerName::UNKNOWN) {
            return compilerPath;
        }
    }
    throw CompilationDatabaseException("Cannot detect compiler");
}

const clang::tooling::CompilationDatabase &
CompilationDatabase::getClangCompilationDatabase() const {
    return *clangCompilationDatabase;
}

const CollectionUtils::FileSet &CompilationDatabase::getAllFiles() const {
    return allFiles;
}

const fs::path &CompilationDatabase::getBuildCompilerPath() const {
    return buildCompilerPath;
}

const std::optional<fs::path> &CompilationDatabase::getResourceDir() const {
    return resourceDir;
}
std::unique_ptr<CompilationDatabase>
CompilationDatabase::autoDetectFromDirectory(fs::path const& SourceDir, std::string &ErrorMessage) {
    LOG_S(INFO) << "in CompilationDatabase::autoDetectFromDirectory";
    auto clangCompilationDatabase = clang::tooling::CompilationDatabase::autoDetectFromDirectory(
        SourceDir.c_str(), ErrorMessage);
    LOG_S(INFO) << "after calling clang::tooling::autoDetectFromDirectory";
    if (clangCompilationDatabase == nullptr) {
        return nullptr;
    }
    return std::make_unique<CompilationDatabase>(std::move(clangCompilationDatabase));
}
