#pragma once

#include <exception>
#include <string>

namespace BackupRestoreAgent
{

class FailedToTransferBackupException: public std::exception
{
public:
    explicit FailedToTransferBackupException(const std::string& message)
        : message(message)
    {
    }

    virtual const char* what() const noexcept
    {
        return message.c_str();
    }

private:
    std::string message;
};

}
