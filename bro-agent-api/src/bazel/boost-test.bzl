def _create_boost_test_wrapper_script(ctx, binary, args):
    return ctx.actions.write(
        content = """\
#!/usr/bin/env bash

set -euo pipefail

BOOST_TEST_ARGS_LINES=$(cat <<'_BOOST_TEST_ARGS_LINES'
    "{args}"
_BOOST_TEST_ARGS_LINES
)

BOOST_TEST=("$(basename "{binary}")")
while read -r arg_line; do
    BOOST_TEST+=("$arg_line")
done <<< "$BOOST_TEST_ARGS_LINES"

cd "$(dirname "{binary}")"
exec "${{BOOST_TEST[@]}}"
""".format(binary = binary.short_path, args = "\n".join(args)),
        output = ctx.outputs.executable,
        is_executable = True,
    )

def _boost_test_test_impl(ctx):
    wrapper = _create_boost_test_wrapper_script(
        ctx,
        ctx.files.binary[0],
        ctx.attr.test_args,
    )
    files = ctx.files.binary
    runfiles = ctx.runfiles(files, collect_data = True)
    return [DefaultInfo(
        executable = wrapper,
        runfiles = runfiles,
    )]

boost_test_test = rule(
    implementation = _boost_test_test_impl,
    attrs = {
        "binary": attr.label(mandatory = True),
        "test_args": attr.string_list(mandatory = False),
        "data": attr.label_list(mandatory = False, default = []),
    },
    test = True,
)
