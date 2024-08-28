def _cc_gmock_impl(ctx):
    src = ctx.file.src
    out_name = ctx.attr.output_dir + "/GMock" + src.basename
    out_file = ctx.actions.declare_file(out_name)
    ctx.actions.run(
        inputs = [
            ctx.file.src,
            ctx.file.generator_script,
            ctx.file.generator_py,
        ],
        outputs = [out_file],
        executable = ctx.file.generator_script,
        arguments = [
            "python2",
            ctx.file.generator_py.path,
            ctx.file.src.path,
            out_file.path,
        ],
    )
    return [
        DefaultInfo(files = depset([out_file])),
    ]

_cc_gmock = rule(
    implementation = _cc_gmock_impl,
    attrs = {
        "src": attr.label(allow_single_file = True, mandatory = True),
        "generator_script": attr.label(
            mandatory = False,
            default = Label("@mgw_test_common//:tools/gmock_gen/gen_mock.sh"),
            executable = True,
            cfg = "host",
            allow_single_file = True,
        ),
        "generator_py": attr.label(
            mandatory = False,
            default = Label("@3pp-bro-googletest//:googlemock/scripts/generator/gmock_gen.py"),
            cfg = "host",
            allow_single_file = True,
        ),
        "output_dir": attr.string(
            mandatory = True,
        ),
    },
)

def cc_gmock(name, src = None, visibility = None, output_dir = "gen_gmock_in"):
    _cc_gmock(
        name = "{}_header".format(name),
        src = src,
        output_dir = output_dir,
    )

    native.cc_library(
        name = name,
        hdrs = [":{}_header".format(name)],
        includes = [output_dir],
        visibility = visibility,
    )
