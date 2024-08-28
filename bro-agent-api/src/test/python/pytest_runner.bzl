def pytest_runner(
    name,
    data=[]):
    native.py_test(
        name = name,
        srcs = [
            name + ".py",
            "//main/python:patched_generated_python",
        ],
        deps = [
            "//main/python:bro_agent",
        ],
        data = data,
        imports = [
            "../../main/python",
        ],
        visibility = ["//visibility:public"],
    )
