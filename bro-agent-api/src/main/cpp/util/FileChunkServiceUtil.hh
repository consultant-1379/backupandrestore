#pragma once

#include <functional>
#include <fstream>


namespace BackupRestoreAgent
{

class IChunkServiceUtil
{
public:
    using Consumer=std::function<void(const char*, int)>;

    virtual ~IChunkServiceUtil(){};

    virtual void processChunks(Consumer chunkConsumer) = 0;
    virtual void setPath(const std::string& path) = 0;
};

/**
 * Consumes file as chunks and lets the implementor do something with the chunks
 */
class FileChunkServiceUtil: public IChunkServiceUtil
{
public:
    explicit FileChunkServiceUtil(const std::string& path)
       : path(path)
    {};
    /**
     * Gives chunks of file from the given path
     *
     * @param chunkConsumer
     *            Consume file in chunks
     */
    void processChunks(Consumer chunkConsumer) override
    {
        std::ifstream fin(path, std::ifstream::binary);
        char* buffer = new char[FILE_CHUNK_SIZE];
        while (fin)
        {
            // Try to read next chunk of data
            fin.read(buffer, FILE_CHUNK_SIZE);
            // Get the number of bytes actually read
            size_t count = fin.gcount();
            // If nothing has been read, break
            if (!count)
                break;
            chunkConsumer(buffer, count);
        }
        delete[] buffer;
    }

    void setPath(const std::string& path) override
    {
        this->path = path;
    }

    static const int FILE_CHUNK_SIZE = 512000;
private:
    std::string path;
};
}
