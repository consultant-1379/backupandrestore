#pragma once

#include <iomanip>
#include <sstream>
#include <openssl/md5.h>

namespace BackupRestoreAgent
{

/**
 * Calculates Checksum
 */
class ChecksumCalculator
{
public:
    /**
     * Creates a checksum calculator.
     */
    ChecksumCalculator();

    /**
     * Reads bytes sent during restore.
     * @param bytes to be read.
     */
    void addBytes(const std::string& content);
    /**
     * Calculates checksum
     * @param content in string format to calculate checksum
     * @return checksum value
     */
    std::string calculateChecksum(const std::string& content);
    /**
     * Calculates checksum.
     * @return checksum.
     */
    std::string getChecksum();

private:
    std::string toHex(unsigned char* bytes);

    MD5_CTX context;
    unsigned char hash[MD5_DIGEST_LENGTH];
};

}
