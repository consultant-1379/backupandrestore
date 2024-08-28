#pragma once

#include <exception>
#include <string>

namespace BackupRestoreAgent
{

class FailedToDownloadException: public std::exception
{
public:
    explicit FailedToDownloadException(const std::string& message)
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
