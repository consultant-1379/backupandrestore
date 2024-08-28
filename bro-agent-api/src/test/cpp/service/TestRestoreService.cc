#include "service/RestoreService.hh"
#include "util/FileStream.hh"

#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include <grpcpp/test/mock_stream.h>

#include "INT_BR_ORCH_DATA_mock.grpc.pb.h"

using namespace BackupRestoreAgent;
using grpc::testing::MockClientReader;
using ::testing::_;
using ::testing::AtLeast;
using ::testing::DoAll;
using ::testing::Exactly;
using ::testing::MockFunction;
using ::testing::Return;
using ::testing::SetArgPointee;
using ::testing::WithArg;

using namespace com::ericsson::adp::mgmt::data;

class StringStream : public IStream
{
public:
    void open(const std::string& path) override
    {
        //Do not need to open anything
        filename = path;
    };
    void write(const std::string& content) override
    {
        stream << content;
    };
    void close() override{
        //Do not need to close anything
    };

    std::stringstream stream;
    std::string filename{ "" };
};

class TestRestoreService : public ::testing::Test
{
public:
    TestRestoreService()
        : stream(new StringStream())
        , rw(new grpc::testing::MockClientReader<RestoreData>())
        , pdata(std::unique_ptr<MockDataInterfaceStub>(new MockDataInterfaceStub()))
        , context(new grpc::ClientContext())
    {
        Fragment* fragment = new Fragment();
        fragment->set_fragmentid("1");
        fragment->set_version("0.1.0");
        fragment->set_sizeinbytes("15");

        metaData.set_agentid("testAgent");
        metaData.set_backupname("testBackup");
        metaData.set_allocated_fragment(fragment);
    };

    ~TestRestoreService()
    {
        delete context;
    };

    RestoreData buildRestoreData(const std::string& file, const std::string& content, const std::string& checksum)
    {
        RestoreData result;
        BackupFileChunk* fileChunk;

        fileChunk = new BackupFileChunk();
        result.set_datamessagetype(DataMessageType::BACKUP_FILE);
        fileChunk->set_filename(file);
        fileChunk->set_content(content);
        fileChunk->set_checksum(checksum);
        result.set_allocated_backupfilechunk(fileChunk);

        return result;
    }

    StringStream* stream;
    grpc::testing::MockClientReader<RestoreData>* rw;
    std::unique_ptr<MockDataInterfaceStub> pdata;
    grpc::ClientContext* context;
    Metadata metaData;
};

TEST_F(TestRestoreService, download_will_read_one_chunk_of_content_from_grpc)
{
    std::string restoreContent = "Lorem ipsum dolor sit amet ...";

    EXPECT_CALL(*rw, Read(_)).Times(4).WillOnce(DoAll(SetArgPointee<0>(buildRestoreData("restored_file", "", "")), Return(true))).WillOnce(DoAll(SetArgPointee<0>(buildRestoreData("", restoreContent, "")), Return(true))).WillOnce(DoAll(SetArgPointee<0>(buildRestoreData("", "", "1F11A06DCE9B5A6BC79422C7D5229F3F")), Return(true))).WillOnce(Return(false));
    EXPECT_CALL(*rw, Finish()).Times(1);
    EXPECT_CALL(*pdata, restoreRaw(_, _)).Times(AtLeast(1)).WillOnce(Return(rw));

    RestoreDataStream restoreStream = pdata->restore(context, metaData);

    RestoreService service(std::move(restoreStream), stream);
    service.download("/tmp");

    EXPECT_EQ("/tmp/restored_file", stream->filename);
    EXPECT_EQ(restoreContent, stream->stream.str());
}

TEST_F(TestRestoreService, download_will_read_several_chunks_of_content_from_grpc)
{
    std::string restoreContent1 = "Lorem ipsum ";
    std::string restoreContent2 = "dolor sit amet ...";

    EXPECT_CALL(*rw, Read(_)).Times(5) //
        .WillOnce(DoAll(SetArgPointee<0>(buildRestoreData("restored_file", "", "")), Return(true))) //
        .WillOnce(DoAll(SetArgPointee<0>(buildRestoreData("", restoreContent1, "")), Return(true))) //
        .WillOnce(DoAll(SetArgPointee<0>(buildRestoreData("", restoreContent2, "")), Return(true))) //
        .WillOnce(DoAll(SetArgPointee<0>(buildRestoreData("", "", "1F11A06DCE9B5A6BC79422C7D5229F3F")), Return(true))) //
        .WillOnce(Return(false));
    EXPECT_CALL(*rw, Finish()).Times(1);
    EXPECT_CALL(*pdata, restoreRaw(_, _)).Times(AtLeast(1)).WillOnce(Return(rw));

    RestoreDataStream restoreStream = pdata->restore(context, metaData);

    RestoreService service(std::move(restoreStream), stream);
    service.download("/tmp");

    EXPECT_EQ("/tmp/restored_file", stream->filename);
    EXPECT_EQ(restoreContent1.append(restoreContent2), stream->stream.str());
}

int main(int argc, char** argv)
{
    // The following line must be executed to initialize Google Mock
    // (and Google Test) before running the tests.
    ::testing::InitGoogleMock(&argc, argv);
    return RUN_ALL_TESTS();
}
