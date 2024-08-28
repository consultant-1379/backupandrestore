#include "fragment/BackupFragmentInformation.hh"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace BackupRestoreAgent;
using ::testing::Exactly;
using ::testing::MockFunction;

class TestBackupFragmentInformation : public ::testing::Test
{
public:
    TestBackupFragmentInformation()
    {
        std::cout << "TC start" << std::endl;
    };

    ~TestBackupFragmentInformation()
    {
        std::cout << "TC stop" << std::endl;
    };

};

TEST_F(TestBackupFragmentInformation, check_that_fragmentinfprmation_class_is_stores_values)
{
    BackupFragmentInformation* fi = new BackupFragmentInformation("1", "0.1.0", "42", "/path/backup/", "/path/customMetaData/");

    EXPECT_EQ(fi->getFragmentId(), "1");
    EXPECT_EQ(fi->getVersion(), "0.1.0");
    EXPECT_EQ(fi->getSizeInBytes(), "42");
    EXPECT_EQ(fi->getBackupFilePath(), "/path/backup/");
    EXPECT_EQ(fi->getCustomMetadataFilePath(), "/path/customMetaData/");

}

int main(int argc, char** argv)
{
    // The following line must be executed to initialize Google Mock
    // (and Google Test) before running the tests.
    ::testing::InitGoogleMock(&argc, argv);
    return RUN_ALL_TESTS();
}