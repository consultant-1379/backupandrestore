#include "fragment/FragmentInformation.hh"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace BackupRestoreAgent;
using ::testing::Exactly;
using ::testing::MockFunction;

class TestFragmentInformation : public ::testing::Test
{
public:
    TestFragmentInformation()
    {
        std::cout << "TC start" << std::endl;
    };

    ~TestFragmentInformation()
    {
        std::cout << "TC stop" << std::endl;
    };

};

TEST_F(TestFragmentInformation, check_that_backupfragmentinformation_class_is_stores_values)
{
    FragmentInformation* fi = new FragmentInformation("1", "0.1.0", "42");

    EXPECT_EQ(fi->getFragmentId(), "1");
    EXPECT_EQ(fi->getVersion(), "0.1.0");
    EXPECT_EQ(fi->getSizeInBytes(), "42");
}

int main(int argc, char** argv)
{
    // The following line must be executed to initialize Google Mock
    // (and Google Test) before running the tests.
    ::testing::InitGoogleMock(&argc, argv);
    return RUN_ALL_TESTS();
}