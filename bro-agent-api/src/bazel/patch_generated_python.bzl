def _patched_generated_python_source_impl(ctx):
    ctx.actions.run(
        outputs = ctx.outputs.outs,
        inputs = ctx.files.srcs,
        executable = ctx.file.patch_script,
        arguments = [
            ctx.files.srcs[0].dirname,
            ctx.outputs.outs[0].dirname,
        ] + [i.basename for i in ctx.files.srcs],
        execution_requirements = {"block-network": "1"},
    )

patched_generated_python_source = rule(
    implementation = _patched_generated_python_source_impl,
    attrs = {
        "patch_script": attr.label(
            mandatory = False,
            allow_single_file = True,
            executable = True,
            cfg = "host",
            default = "//bazel:patch_generated_python.sh",
        ),
        "srcs": attr.label_list(mandatory = True),
        "outs": attr.output_list(mandatory = True),

    },
)

def patched_generated_python_files(name, srcs, outs):
    patched_generated_python_source(
        name = name,
        srcs = srcs,
        outs = outs,
    )
