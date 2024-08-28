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
package com.ericsson.adp.mgmt.backupandrestore.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;

import com.ericsson.adp.mgmt.backupandrestore.exception.KeyStoreGenerationException;
import com.ericsson.adp.mgmt.backupandrestore.persist.FileService;

/**
 * Utility class for TLS certificate handling
 */
public class TLSCertUtils {

    private static final Logger log = LogManager.getLogger(TLSCertUtils.class);
    private static final String BRO_CERT_ISSUE_CATEGORY = "BRO-Certificate-Issue";
    private static final String CLASS_STRING = TLSCertUtils.class.getName();

    private TLSCertUtils() {}

    /**
     * Checks the given path for certificate files and returns the preferred file path.
     * The preferred file is the first one that ends with ".crt" for "cert", "ca" file types,
     * or ".key" for "priv" file type, and does not start with a dot.
     * If no such file is found, the original path is returned.
     *
     * @param path the path to check for certificate files
     * @param fileType the type of the file to look for ("cert", "ca", or "priv")
     * @return the path of the preferred file, or the original path if no preferred file is found
     */
    public static String checkCertsInPathAndTakePreference(final String path, final String fileType) {
        if (!FileService.isPathValidReadable(path)) {
            log.debug(fileType + " path is not valid or not readable: " + path);
        }
        Optional<String> preferenceFile = Optional.empty();
        final Optional<File> parent = Optional.ofNullable(new File(path).getParentFile());
        List<String> files = new ArrayList<>();

        try {
            if (parent.isPresent()) {
                files = listFilesInDirectory(parent.get().getPath());
            }
        } catch (Exception e) {
            log.debug("Failed to get files in parent directory for specified path {}. For {}", path, e);
        }

        // check if tls.crt, tls.key or ca.crt exist
        // avoid incorrect certificate files like .tls.crt, .tls.key, .ca.crt, etc.
        if (!files.isEmpty()) {
            if ("cert".equals(fileType)) {
                preferenceFile = files.stream().filter(fileName ->
                    !fileName.endsWith(".tls.crt") && fileName.endsWith(".crt")).findFirst();
            } else if ("ca".equals(fileType)) {
                preferenceFile = files.stream().filter(fileName ->
                    !fileName.endsWith(".ca.crt") && fileName.endsWith(".crt")).findFirst();
            } else if ("priv".equals(fileType)) {
                preferenceFile = files.stream().filter(fileName ->
                    !fileName.endsWith(".tls.key") && fileName.endsWith(".key")).findFirst();
            }
        }

        // if no valid .crt or .key is found then check the specified path by user and set that as the preference
        if (preferenceFile.isEmpty()) {
            preferenceFile =  Optional.of(path.toString());
        }
        return preferenceFile.get();
    }

    /**
     * Lists all files in the given directory.
     * This method does not list subdirectories or files within subdirectories.
     *
     * @param dir the directory to list files from
     * @return a list of absolute paths to the files in the directory
     * @throws IOException if an I/O error occurs when opening the directory
     */
    public static List<String> listFilesInDirectory(final String dir) throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(dir))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }


    /**
     * Checks if the given PEM certificate is expired
     * (by checking certificate from filepath)
     * @param certificatePath path of the certificate file
     * @return true if certificate is expired, false otherwise
     */
    public static Boolean isCertificateExpired (final String certificatePath) {
        try (
            PEMParser parser = new PEMParser(new BufferedReader(new FileReader(certificatePath)))
        ) {
            final String certName = Paths.get(certificatePath).getFileName().toString();
            Object object;
            // NOTE there should be only one certificate in the path
            while ((object = parser.readObject()) != null) {
                if (object instanceof X509CertificateHolder) {
                    if (isCertificateExpired((X509CertificateHolder) object, certName)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            throw new KeyStoreGenerationException(
                "Error while retrieving the certificate: " + certificatePath, e);
        }
        return false;
    }

    /**
     * Checks if the given PEM certificate is expired
     * (by checking certificate object -X509CertificateHolder)
     * @param certificate the certificate to check
     * @param certName the name of the certificate
     * @return true if certificate is expired, false otherwise
     */
    public static Boolean isCertificateExpired (final X509CertificateHolder certificate, final String certName) {
        Instant expirationDate = null;
        final Date date = certificate.getNotAfter();
        final String subjectDN = certificate.getSubject().toString();
        final String issuerDN = certificate.getIssuer().toString();
        if (date != null) {
            expirationDate = date.toInstant();
            if (expirationDate.getEpochSecond() <= Instant.now().getEpochSecond()) {
                SecurityEventLogger.logSecurityErrorEvent(
                    CLASS_STRING, BRO_CERT_ISSUE_CATEGORY,
                    "Certificate not Valid: certificate " + certName + " expired on "
                    + expirationDate.toString() + ", subject name: " + subjectDN
                    + ", issuer name: " + issuerDN);
                return true;
            }
        } else {
            SecurityEventLogger.logSecurityErrorEvent(
                CLASS_STRING, BRO_CERT_ISSUE_CATEGORY,
                "Certificate not Valid: Failed to get "
                + "expiration date for certificate: <" + certName
                + ">, subject name: " + subjectDN + ", issuer name: " + issuerDN);
        }
        return false;
    }
}
