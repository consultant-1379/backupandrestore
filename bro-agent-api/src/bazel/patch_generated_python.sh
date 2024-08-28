#!/usr/bin/env bash

set -euo pipefail

INPUT_DIR=$1
OUTPUT_DIR=$2
shift 2

function fetch_file_list() {
    python3 -c \
'
import os, sys;

GENERATED_FILES = []
for i in sys.stdin.readlines():
    GENERATED_FILES.append(i.split("_pb2")[0])
for i in set(GENERATED_FILES):
    print("{}_pb2".format(i))
'
}

# Two Bazel rules cannot have the same output files, which implies source files
# and output files of a rule cannot be the same. So we copy all source files to
# a different target directory, and patch them there.

cp \
    --dereference \
    --no-preserve=mode \
    --recursive \
    --target-directory "${OUTPUT_DIR}" \
    "${INPUT_DIR}"/*

GENERATED_FILES=($(printf '%s\n' "$@" | fetch_file_list))

for file in "${GENERATED_FILES[@]}"
do
    find "${OUTPUT_DIR}" -type f -exec sed -i -e "s/^import ${file}/import bro_agent.generated.${file}/g" {} \;
done
