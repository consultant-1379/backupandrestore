load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

def bro_include_boost(bro_repo_name = "@agent"):
    http_archive(
        name = "3pp-bro-boost",
        sha256 = "da3411ea45622579d419bfda66f45cd0f8c32a181d84adfa936f5688388995cf",
        build_file = bro_repo_name + "//bazel:boost_BUILD.bazel",
        urls = ["https://arm.sero.gic.ericsson.se/artifactory/proj-vims-generic-local/3pp/boost/boost_1_68_0.tar.gz"],
        strip_prefix = "boost_1_68_0",
    )

def bro_include_googletest(bro_repo_name = "@agent"):
    http_archive(
        name = "3pp-bro-googletest",
        sha256 = "9bf1fe5182a604b4135edc1a425ae356c9ad15e9b23f9f12a02e80184c3a249c",
        build_file = bro_repo_name + "//bazel:googletest_BUILD.bazel",
        urls = ["https://arm.sero.gic.ericsson.se/artifactory/proj-vims-generic-local/3pp/googletest/googletest-release-1.8.1.tar.gz"],
        strip_prefix = "googletest-release-1.8.1",
        workspace_file = bro_repo_name + "//bazel:googletest_WORKSPACE",
    )

def bro_include_grpc(bro_repo_name = "@agent"):
    http_archive(
        # Can not rename this becuase this name hard wired inside grpc :(
        name = "com_github_grpc_grpc",
        sha256 = "3ccc4e5ae8c1ce844456e39cc11f1c991a7da74396faabe83d779836ef449bce",
        urls = ["https://arm.sero.gic.ericsson.se/artifactory/proj-vims-generic-local/3pp/grpc/grpc-1.27.0.tar.gz"],
        strip_prefix = "grpc-1.27.0",
        patches = [
            bro_repo_name + "//bazel:grpc_deps.patch",
            bro_repo_name + "//bazel:python_rules.patch",
            bro_repo_name + "//bazel:protobuf.patch",
        ],
    )

def bro_include_rules_proto(bro_repo_name = "@agent"):
    http_archive(
        name = "rules_proto",
        sha256 = "602e7161d9195e50246177e7c55b2f39950a9cf7366f74ed5f22fd45750cd208",
        strip_prefix = "rules_proto-97d8af4dc474595af3900dd85cb3a29ad28cc313",
        urls = [
            "https://arm.sero.gic.ericsson.se/artifactory/proj-vims-generic-local/3pp/bazel/rules_proto-97d8af4dc474595af3900dd85cb3a29ad28cc313.tar.gz",
            "https://mirror.bazel.build/github.com/bazelbuild/rules_proto/archive/97d8af4dc474595af3900dd85cb3a29ad28cc313.tar.gz",
        ],
    )

def bro_include_rules_python(bro_repo_name = "@agent"):
    http_archive(
        name = "rules_python",
        sha256 = "fa53cc0afe276d8f6675df1a424592e00e4f37b2a497e48399123233902e2e76",
        url = "https://arm.sero.gic.ericsson.se/artifactory/proj-vims-generic-local/3pp/bazel/rules_python-0.0.1.tar.gz",
        strip_prefix = "rules_python-0.0.1",
    )

def bro_include_bro_proto(bro_repo_name = "@agent"):
    native.new_local_repository(
        name = "adp_bro",
        path = "main/proto",
        build_file = bro_repo_name + "//bazel:adp_bro_protos_BUILD.bazel",
    )
