#!/bin/bash

# Create builder
#
create_builder() {
        zypper ar --no-check --gpgcheck-strict -f "${CBO_DEVENV_REPO}/${CBO_VERSION}" CBO_DEVENV
        zypper --gpg-auto-import-keys refresh
        zypper -n install --no-recommends -l buildah skopeo curl
        sed -i 's/^driver =.*/driver="vfs"/' /etc/containers/storage.conf
        zypper rr CBO_DEVENV
}

# Create a container from the micro-CBO based image
# The root directory is available with $rootdir
#
mount_microcbo_container() {
        echo "MOUNT_MICROCBO_CONTAIER"
        zypper ar --no-check --gpgcheck-strict -f "${CBO_REPO}/${CBO_VERSION}" CBO_REPO
        zypper --gpg-auto-import-keys refresh
        container=$(buildah from docker-daemon:"$MICROCBO_IMAGE")
        rootdir=$(buildah mount "$container")

        mkdir -p "$rootdir"/proc/ "$rootdir"/dev/
        mount -t proc /proc "$rootdir"/proc/
        mount --rbind /dev "$rootdir"/dev/
}

# Build the image with additional packages required for CIS-CAT scanning
#
build_ciscat_scan_image() {
        echo "build_ciscat_scan_image"
        zypper -n --installroot "$rootdir" install --no-recommends -l \
                gawk \
                hostname \
                iproute2 \
                rpm \
                util-linux \
                which

}

# Commit and upload the created image from container storage to local docker daemon
#
upload_image() {
        umount "$rootdir"/proc/
        umount -l "$rootdir"/dev/
        buildah commit -f docker "$container" "${CISCAT_SCAN_IMAGE}"
        buildah rm "$container"
        if (echo "${CISCAT_SCAN_IMAGE}" | grep -qE '^(armdocker)|(selndocker)'); then
                skopeo copy containers-storage:"${CISCAT_SCAN_IMAGE}" \
                        docker-daemon:"${CISCAT_SCAN_IMAGE}"
        else
                skopeo copy containers-storage:localhost/"${CISCAT_SCAN_IMAGE}" \
                        docker-daemon:"${CISCAT_SCAN_IMAGE}"
        fi
}

create_builder
mount_microcbo_container
build_ciscat_scan_image
upload_image