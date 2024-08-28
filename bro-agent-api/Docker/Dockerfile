FROM armdocker.rnd.ericsson.se/proj-ldc/common_base_os_release/sles:6.17.0-11

LABEL GIT_COMMIT=unknown

ARG PACKAGE_REPO_URL=https://arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/6.17.0-11

ARG PACKAGE_REPO_DEV=https://arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/6.17.0-11

ARG PACKAGE_REPO_SDK=https://arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/adp-dev/adp-build-env/6.17.0-11

RUN zypper ar -C -G -f $PACKAGE_REPO_URL?ssl_verify=no LDC-SLES15 \
    && zypper --non-interactive refresh -f \
    && zypper install -l -y \
        --auto-agree-with-licenses \
        --no-confirm \
        --no-recommends \
          ca-certificates-mozilla

RUN zypper ar -C -G -f $PACKAGE_REPO_DEV?ssl_verify=no CBO-DEV \
    && zypper --non-interactive refresh -f \
    && zypper install -l -y \
        --auto-agree-with-licenses \
        --no-confirm \
        --no-recommends \
          ca-certificates-mozilla

RUN zypper ar -C -G -f $PACKAGE_REPO_SDK?ssl_verify=no CBO-SDK \
    && zypper --non-interactive refresh -f \
    && zypper install -l -y \
        --auto-agree-with-licenses \
        --no-confirm \
        --no-recommends \
          ca-certificates-mozilla

RUN zypper install -l -y \
        --auto-agree-with-licenses \
        --no-confirm \
        --no-recommends \
          clang7 \
          curl \
          gcc-c++ \
          java-1_8_0-openjdk-headless \
          patch \
          python3-devel \
          python3-pip \
          wget


RUN pip3 install --no-cache-dir \
        bandit==1.7.1 \
        flake8 \
        grpcio-tools \
        pytest

RUN cp /usr/bin/python3 /usr/bin/python

ARG BAZEL_VERSION=${BAZEL_VERSION:-0.29.0}
RUN \
  wget -qO /usr/bin/bazel-real "https://github.com/bazelbuild/bazel/releases/download/${BAZEL_VERSION}/bazel-${BAZEL_VERSION}-linux-x86_64" && \
  wget -qO /usr/bin/bazel "https://raw.githubusercontent.com/bazelbuild/bazel/${BAZEL_VERSION}/scripts/packages/bazel.sh" && \
  chmod +x /usr/bin/bazel /usr/bin/bazel-real

RUN \
  wget -qO /usr/bin/run-clang-format https://raw.githubusercontent.com/Sarcasm/run-clang-format/master/run-clang-format.py && \
  chmod +x /usr/bin/run-clang-format

CMD /bin/bash
