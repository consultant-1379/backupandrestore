#!/bin/bash
# Copyright (c) Ericsson AB 2023. All rights reserved.
#
# The information in this document is the property of Ericsson.
#
# Except as specifically authorized in writing by Ericsson, the
# receiver of this document shall keep the information contained
# herein confidential and shall protect the same in whole or in
# part from disclosure and dissemination to third parties.
#
# Disclosure and disseminations to the receivers employees shall
# only be made on a strict need to know basis.
#

## build_app_layer
##
##   Build application layer, same as with Dockerfile
##
build_app_layer() {
    # Enable debugging
    set -x

    # user_id is based on the container name - DR-D1123-122
    user_id=287330

    # Add user to passwd file
    echo "$user_id:x:$user_id:$user_id:An Identity for eric-ctrl-bro:/home/bro:/bin/bash" >> "$rootdir/etc/passwd"
    echo "$user_id:!::0:::::" >> "$rootdir/etc/shadow"

    # Create br, bro and eoi_model directories
    mkdir -p "$rootdir/opt/ericsson/br"
    mkdir -p "$rootdir/var/opt/ericsson/br/eoi_model"
    mkdir -p "$rootdir/home/bro"
    mkdir -p "$rootdir/bro"

    # Copy the needed files
    # The Eclipse Transformer generates an additional JAR file as part of its transformation process
    # the extra jar file generated needs to be excluded
    rm service/Docker/target/backupandrestore-jakarta*.jar
    cp service/Docker/target/backupandrestore-*.jar "$rootdir/opt/ericsson/br/eric-ctrl-bro.jar"
    cp -r service/Docker/conf/scripts/schema/source/* "$rootdir/var/opt/ericsson/br/eoi_model/"
    cp service/Docker/conf/scripts/entrypoint.sh "$rootdir/var/opt/ericsson/br/entrypoint.sh"
    cp service/Docker/conf/scripts/StartupProbe.sh "$rootdir/bin"
    cp service/Docker/conf/scripts/LivenessProbe.sh "$rootdir/bin"
    cp service/Docker/conf/scripts/ReadinessProbe.sh "$rootdir/bin"
    cp httpprobe/build/bin/httpprobe "$rootdir/bin"

    # Change ownership and permissions
    chmod +x "$rootdir/var/opt/ericsson/br/entrypoint.sh"
    chown -R "$user_id:$user_id" "$rootdir/opt/ericsson/br" \
    && chmod -R g+rwx "$rootdir/opt/ericsson/br"
    chown -R "$user_id":0 "$rootdir/usr/local/bin $rootdir/home/bro $rootdir/bro" \
    && chmod -R g+rwx "$rootdir/usr/local/bin $rootdir/home/bro $rootdir/bro"

    # Add CBO repository and refresh package list
    zypper ar --no-check --gpgcheck-strict -f "${CBO_REPO}/${CBO_VERSION}" CBO_REPO
    zypper --gpg-auto-import-keys refresh

    # Install the required packages
    zypper -n --installroot "$rootdir" in --auto-agree-with-licenses --no-confirm \
        java-17-openjdk-headless

    # Save info about the packages
    zypper -n --installroot "$rootdir" se -si > "$rootdir/.zypper-installed-bro"
    rpm --root "$rootdir" -qa > "$rootdir/.rpm-installed-bro"

    # Apply image configuration settings
    buildah config \
        --label com.ericsson.product-number="CXC2012181" \
        --label org.opencontainers.image.title="Backup and Restore Orchestrator" \
        --label org.opencontainers.image.created="$BUILD_TIME" \
        --label org.opencontainers.image.revision="$COMMIT" \
        --label org.opencontainers.image.vendor="Ericsson" \
        --label org.opencontainers.image.version="$BRO_VERSION" \
        --port 3000/tcp \
        --port 7001/tcp \
        --port 7002/tcp \
        --port 7003/tcp \
        --port 7004/tcp \
        --port 7005/tcp \
        --user $user_id \
        --entrypoint "/var/opt/ericsson/br/entrypoint.sh" $container

    # Remove the installed packages
    zypper clean --all \
    && zypper rr CBO_REPO

    set +x
}

## mount_microcbo_container
##
## Create a container from microcbo
##
## The root directory is available with $rootdir
##
mount_microcbo_container() {
    container=$(buildah from docker-daemon:"$MICROCBO_IMAGE")
    rootdir=$(buildah mount "$container")

    mkdir -p "$rootdir/proc/" "$rootdir/dev/"
    mount -t proc /proc "$rootdir/proc/"
    mount --rbind /dev "$rootdir/dev/"
}

## upload_image
##
##   Commit and upload the created image
##
upload_image() {
    umount "$rootdir/proc/"
    umount -l "$rootdir/dev/"

    buildah commit -f docker "$container" "$BRO_REGISTRY:$BRO_VERSION"
    buildah images

    # Copy the image to the local docker-daemon
    skopeo copy \
        containers-storage:"$BRO_REGISTRY:$BRO_VERSION" \
        docker-daemon:"$BRO_REGISTRY:$BRO_VERSION"
}

## create_builder
##
##   Create builder layer
##
create_builder() {
    zypper ar --no-check --gpgcheck-strict -f "${CBO_DEVENV_REPO}/${CBO_VERSION}" CBO_DEVENV
    zypper --gpg-auto-import-keys refresh

    # install the required tools
    zypper -n install --no-recommends -l buildah skopeo util-linux
    sed -i 's/^driver =.*/driver="vfs"/' /etc/containers/storage.conf
    zypper rr CBO_DEVENV
}

# exit on error
set -o errexit -o pipefail
trap 'echo "ERROR: Interrupted" >&2; exit 1' SIGINT

# main functions
create_builder
mount_microcbo_container
build_app_layer
upload_image
