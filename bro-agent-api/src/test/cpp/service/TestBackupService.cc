#include "service/BackupService.hh"

#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include <grpcpp/test/mock_stream.h>
#include <sstream>

#include "INT_BR_ORCH_DATA_mock.grpc.pb.h"

using namespace BackupRestoreAgent;
using grpc::testing::MockClientWriter;
using ::testing::_;
using ::testing::AtLeast;
using ::testing::DoAll;
using ::testing::Exactly;
using ::testing::MockFunction;
using ::testing::Return;
using ::testing::WithArg;

using namespace com::ericsson::adp::mgmt::data;

class StringChunkServiceUtil: public IChunkServiceUtil
{
public:
    void processChunks(Consumer chunkConsumer) override
    {
        int FILE_CHUNK_SIZE = 15;
        std::stringstream fin(content);
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

    void setPath(const std::string& path){};

    void setContent(const std::string& content)
    {
        this->content = content;
    }

private:
    std::string content;

};

class TestBackupService : public ::testing::Test
{
public:
    TestBackupService()
        : sChunk(new StringChunkServiceUtil())
        , rw(new grpc::testing::MockClientWriter<BackupData>())
        , pdata(std::unique_ptr<MockDataInterfaceStub>(new MockDataInterfaceStub()))
        , context(new grpc::ClientContext()){};

    ~TestBackupService()
    {
        delete context;
    };

    StringChunkServiceUtil* sChunk;
    grpc::testing::MockClientWriter<BackupData>* rw;
    std::unique_ptr<MockDataInterfaceStub> pdata;
    grpc::ClientContext* context;
    google::protobuf::Empty empty;
};

int SIZE_OF_CONTENT = 0;
std::string sentContent = "";
std::string sentCustomMetadataContent = "";

ACTION(checkMsg)
{
    if (arg0.backupfilechunk().content() != "")
    {
        sentContent.append(arg0.backupfilechunk().content().substr(0, SIZE_OF_CONTENT));
    }
    if (arg0.custommetadatafilechunk().content() != "")
    {
        sentCustomMetadataContent.append(arg0.custommetadatafilechunk().content().substr(0, SIZE_OF_CONTENT));
    }
}

TEST_F(TestBackupService, backup_will_send_content_over_grpc)
{
    std::string backupContent = "Lorem ..";
    sentContent = "";
    SIZE_OF_CONTENT = backupContent.length();
    sChunk->setContent(backupContent);

    EXPECT_CALL(*rw, Write(_, _)).Times(4).WillRepeatedly(DoAll(WithArg<0>(checkMsg()), Return(true)));
    EXPECT_CALL(*rw, WritesDone()).Times(1);
    EXPECT_CALL(*rw, Finish()).Times(1);
    EXPECT_CALL(*pdata, backupRaw(_, _)).Times(AtLeast(1)).WillOnce(Return(rw));

    BackupDataStream backupStream = pdata->backup(context, &empty);

    BackupService service(std::move(backupStream), sChunk);
    service.backup(BackupFragmentInformation("1", "0.1.0", "42", "/path/backup", ""), "agentId", "backupName");

    EXPECT_EQ(sentContent, backupContent);
}

TEST_F(TestBackupService, backup_will_send_several_content_chunks_over_grpc)
{
    std::string backupContent = "Lorem ipsum dolor sit amet ...";
    sentContent = "";
    SIZE_OF_CONTENT = backupContent.length();
    sChunk->setContent(backupContent);

    EXPECT_CALL(*rw, Write(_, _)).Times(5).WillRepeatedly(DoAll(WithArg<0>(checkMsg()), Return(true)));
    EXPECT_CALL(*rw, WritesDone()).Times(1);
    EXPECT_CALL(*rw, Finish()).Times(1);
    EXPECT_CALL(*pdata, backupRaw(_, _)).Times(AtLeast(1)).WillOnce(Return(rw));

    BackupDataStream backupStream = pdata->backup(context, &empty);

    BackupService service(std::move(backupStream), sChunk);
    service.backup(BackupFragmentInformation("1", "0.1.0", "42", "/path/backup", ""), "agentId", "backupName");

    EXPECT_EQ(sentContent, backupContent);
}

TEST_F(TestBackupService, backup_will_send_content_and_custommetadata_over_grpc)
{
    std::string backupContent = "Lorem ..";
    sentContent = "";
    sentCustomMetadataContent = "";
    SIZE_OF_CONTENT = backupContent.length();
    sChunk->setContent(backupContent);

    EXPECT_CALL(*rw, Write(_, _)).Times(7).WillRepeatedly(DoAll(WithArg<0>(checkMsg()), Return(true)));
    EXPECT_CALL(*rw, WritesDone()).Times(1);
    EXPECT_CALL(*rw, Finish()).Times(1);
    EXPECT_CALL(*pdata, backupRaw(_, _)).Times(AtLeast(1)).WillOnce(Return(rw));

    BackupDataStream backupStream = pdata->backup(context, &empty);

    BackupService service(std::move(backupStream), sChunk);
    service.backup(BackupFragmentInformation("1", "0.1.0", "42", "/path/backup", "/path/custommetadata"), "agentId", "backupName");

    EXPECT_EQ(sentContent, backupContent);
    EXPECT_EQ(sentCustomMetadataContent, backupContent);
}

int main(int argc, char** argv)
{
    // The following line must be executed to initialize Google Mock
    // (and Google Test) before running the tests.
    ::testing::InitGoogleMock(&argc, argv);
    return RUN_ALL_TESTS();
}
