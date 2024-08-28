#include "FileStream.hh"

namespace BackupRestoreAgent
{

void FileStream::open(const std::string& path)
{
    stream.open(path, std::ofstream::out | std::ofstream::binary);
}

void FileStream::write(const std::string& content)
{
    stream << content;
}

void FileStream::close()
{
    stream.close();
}
}
