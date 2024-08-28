#pragma once

#include <fstream>

#include "agent/OrchestratorGrpcChannel.hh"
#include "fragment/BackupFragmentInformation.hh"
#include "util/ChecksumCalculator.hh"
#include "util/FileStream.hh"

namespace BackupRestoreAgent
{

/**
 * Stores fragment data sent in restore data stream
 */
class RestoreService
{
public:
    /**
     * Stores fragment data sent in restore data stream
     *
     * @param restoreLocation
     *            the directory to download fragments to
     */
    explicit RestoreService(RestoreDataStream restoreStream, IStream* outStream);
    ~RestoreService();

    /**
     * Download files comprising each fragment
     *
     * @param restoreLocation
     *            the directory to download fragments to
     */
    void download(const std::string& restoreLocation);

private:
    void parseServerData(const com::ericsson::adp::mgmt::data::RestoreData& server_data, const std::string& restoreLocation);
    template<class T, class FType>
    void parseFileData(const T& server_data, FType getfilechunk, const std::string& restoreLocation);

    RestoreDataStream restoreStream;
    std::string restoreFile{""};
    ChecksumCalculator calculator;
    IStream* outStream;
};

}
