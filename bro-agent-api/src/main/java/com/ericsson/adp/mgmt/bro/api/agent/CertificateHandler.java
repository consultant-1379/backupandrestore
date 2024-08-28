/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.bro.api.agent;

import com.ericsson.adp.mgmt.bro.api.util.CertificateType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class to handle certificates in relation to DR 133
 * */
public class CertificateHandler {
    private static final Logger log = LogManager.getLogger(CertificateHandler.class);

    /**
     * Prevents external instantiation.
     */
    private CertificateHandler() {
    }

    /**
     * DR-D1123-133 states that ADP services shall support loading certificates from standard filenames in secrets
     * 'tls.key' - for service private key
     * 'tls.crt' - for service leaf certificate and chain
     * 'ca.crt' - for trusted CA certificate bundle
     * this method will check for the defined path in OrchestratorConnectionInformation and check the parent directory for
     * the above formats depending on the fileType and set the path in OrchestratorConnectionInformation
     * @param information - orchestratorConnectionInformation
     * @param path - path of what is currently being checked
     * @param fileType - type of certificate
     * @return preferenceFile
     * */
    public static  Optional<String> checkCertsInPathAndTakePreference(final OrchestratorConnectionInformation information,
                                                                      final String path, final CertificateType fileType) {
        Optional<String> preferenceFile = Optional.empty();
        final File parent = new File(path).getParentFile();
        List<String> files = new ArrayList<>();

        try {
            files = listFilesInDirectory(parent.getPath());
        } catch (Exception e) {
            log.warn("Failed to get files in parent directory for specified path {}. For {}", path, e);
        }

        // check if tls.crt, tls.key or ca.crt exist
        // since filename in files are absolute path, specific
        // filter added to filter out files like /home/.../resource/.ca.crt
        if (!files.isEmpty()) {
            if (CertificateType.PUBLIC_KEY.equals(fileType)) {
                preferenceFile = files.stream().filter(fileName ->
                    !fileName.endsWith(".tls.crt") && fileName.endsWith("tls.crt")).findFirst();
            } else if (CertificateType.CERTIFICATE_AUTHORITY.equals(fileType)) {
                preferenceFile = files.stream().filter(fileName ->
                    !fileName.endsWith(".ca.crt") && fileName.endsWith("ca.crt")).findFirst();
            } else if (CertificateType.PRIVATE_KEY.equals(fileType)) {
                preferenceFile = files.stream().filter(fileName ->
                    !fileName.endsWith(".tls.key") && fileName.endsWith("tls.key")).findFirst();
            }
        }

        if (preferenceFile.isEmpty()) {
            preferenceFile = Optional.of(path);
        }

        if (notCurrentCertificatePath(information, preferenceFile.get())) {
            log.info("Selected path {} for {}", preferenceFile.get(), fileType);
        }
        setCertificateOrchestratorConnectionInformation(information, fileType, preferenceFile.get());
        return preferenceFile;
    }

    private static void setCertificateOrchestratorConnectionInformation(final OrchestratorConnectionInformation information,
                                                                 final CertificateType fileType, final String path) {
        if (CertificateType.PUBLIC_KEY.equals(fileType)) {
            information.setClientCertificatePath(path);
        } else if (CertificateType.CERTIFICATE_AUTHORITY.equals(fileType)) {
            information.setCertificateAuthorityPath(path);
        } else if (CertificateType.PRIVATE_KEY.equals(fileType)) {
            information.setClientPrivKeyPath(path);
        }
    }

    private static boolean notCurrentCertificatePath(final OrchestratorConnectionInformation information, final String pathOfSelectedFile) {
        final boolean result = information.getValidPaths().stream().noneMatch(f -> f.equals(pathOfSelectedFile));
        return result;
    }

    private static List<String> listFilesInDirectory(final String dir) throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(dir))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }
}
