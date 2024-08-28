#include "util/FileChunkServiceUtil.hh"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace BackupRestoreAgent;
using ::testing::Exactly;
using ::testing::MockFunction;

class TestFileChunkServiceUtil : public ::testing::Test
{
public:
    TestFileChunkServiceUtil()
    {
        std::cout << "TC start" << std::endl;
        createFile("/tmp/small", 32);
        createFile("/tmp/big", 2*FileChunkServiceUtil::FILE_CHUNK_SIZE); // comes from FileChunkServiceUtil
        createFile("/tmp/bigger", 2*FileChunkServiceUtil::FILE_CHUNK_SIZE+16);
    };

    ~TestFileChunkServiceUtil()
    {
        std::cout << "TC stop" << std::endl;
    };

    void createFile(std::string fileName,  int size)
    {
        std::ofstream dataFile;
        dataFile.open (fileName);
        for (int i =0; i<size/16; ++i)
        {
            dataFile << "Lorem ipsum ....";
        }
        dataFile.close();
    }
};

TEST_F(TestFileChunkServiceUtil, check_if_consumer_callback_is_called_once_if_file_size_smaller_than_chunck_size)
{
    std::string smallFile{"/tmp/small"};
    FileChunkServiceUtil fileChunkServiceUtil(smallFile);
    int consumerCalled = 0;
    FileChunkServiceUtil::Consumer consumer = [&](const char* chunk, int chunkSize)
    {
        consumerCalled++;
    };

    fileChunkServiceUtil.processChunks(consumer);
    EXPECT_EQ(consumerCalled, 1);
}

TEST_F(TestFileChunkServiceUtil, check_if_consumer_callback_is_called_more_than_once_if_file_size_bigger_than_chunck_size)
{
    std::string bigFile{"/tmp/big"};
    FileChunkServiceUtil fileChunkServiceUtil(bigFile);
    int consumerCalled = 0;
    FileChunkServiceUtil::Consumer consumer = [&](const char* chunk, int chunkSize)
    {
        consumerCalled++;
    };

    fileChunkServiceUtil.processChunks(consumer);
    EXPECT_GE(consumerCalled, 2);
}


TEST_F(TestFileChunkServiceUtil, check_if_file_size_twice_bigger_than_chunck_size_plus_one)
{
    std::string bigFile{"/tmp/bigger"};
    FileChunkServiceUtil fileChunkServiceUtil(bigFile);
    int consumerCalled = 0;
    int fileSize = 0;
    FileChunkServiceUtil::Consumer consumer = [&](const char* chunk, int chunkSize)
    {
        consumerCalled++;
        fileSize+=chunkSize;
    };

    fileChunkServiceUtil.processChunks(consumer);
    EXPECT_EQ(consumerCalled, 3);
    EXPECT_EQ(fileSize, 2*FileChunkServiceUtil::FILE_CHUNK_SIZE+16);
}

int main(int argc, char** argv)
{
    // The following line must be executed to initialize Google Mock
    // (and Google Test) before running the tests.
    ::testing::InitGoogleMock(&argc, argv);
    return RUN_ALL_TESTS();
}
