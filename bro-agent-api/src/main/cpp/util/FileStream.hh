#pragma once

#include <fstream>

namespace BackupRestoreAgent
{

class IStream
{
public:
    virtual ~IStream(){};

    virtual void open(const std::string& path) = 0;
    virtual void write(const std::string& content) = 0;
    virtual void close() = 0;
};

class FileStream : public IStream
{
public:
    FileStream(){};
    ~FileStream(){};

    void open(const std::string& path) override;
    void write(const std::string& content) override;
    void close() override;

private:
    std::ofstream stream;
};

}
