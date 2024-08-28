FROM armdocker.rnd.ericsson.se/proj-ldc/common_base_os_release/sles:6.17.0-11

LABEL GIT_COMMIT=commitid

ARG PACKAGE_REPO_URL=https://arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/6.17.0-11

RUN zypper ar -C -G -f $PACKAGE_REPO_URL?ssl_verify=no LDC-SLES15 \
    && zypper --non-interactive ref -f -r LDC-SLES15 \
    && zypper in -l -y java-11-openjdk-headless

# Create a backupdata directory and copy the contents of the data directory to it
RUN mkdir servicedata
COPY data/ servicedata/

# Align with non-root security policy.
RUN echo "287330:x:287330:0:An Identity for eric-test-service-basic:/home/bro:/bin/bash" >> /etc/passwd

RUN mkdir -p /home/bro \
  && chown -R 287330:0 /usr/local/bin /home/bro \
  && chmod -R g+rwx /usr/local/bin /home/bro

USER 287330
