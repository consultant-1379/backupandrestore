#include "ChecksumCalculator.hh"

namespace BackupRestoreAgent
{

ChecksumCalculator::ChecksumCalculator()
    : hash{ 0 }
{
    MD5_Init(&context);
}

void ChecksumCalculator::addBytes(const std::string& content)
{
    MD5_Update(&context, content.c_str(), content.length());
}

std::string ChecksumCalculator::calculateChecksum(const std::string& content)
{
    unsigned char hash[MD5_DIGEST_LENGTH];
    MD5((unsigned char*)content.c_str(), content.size(), hash);
    return toHex(hash);
}

std::string ChecksumCalculator::getChecksum()
{
    MD5_Final(hash, &context);
    return toHex(hash);
}

std::string ChecksumCalculator::toHex(unsigned char* bytes)
{
    std::stringstream ss;
    for (int i = 0; i < MD5_DIGEST_LENGTH; i++)
    {
        ss << std::hex << std::setw(2) << std::setfill('0') << std::uppercase << (int)bytes[i];
    }
    return ss.str();
}
}
