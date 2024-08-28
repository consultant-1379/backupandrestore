/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.time.ZoneId;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.boot.Banner;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

/**
 * Starts Backup and Restore application.
 */
@SpringBootApplication
@EnableEncryptableProperties
@EnableCaching
@EnableRetry
@EnableConfigurationProperties
public class BackupAndRestoreApplication {

    private static final Logger log = LogManager.getLogger(BackupAndRestoreApplication.class);
    private String version;

    /**
     * Starts backup and restore rest application.
     *
     * @param args
     *            not used.
     */
    public static void main(final String[] args) {
        if (args.length > 0) {
            expandIPv6(args[0]);
        } else {
            log.debug("The IP address from System.getevn() was {}", System.getenv("KUBERNETES_SERVICE_HOST"));
            new BackupAndRestoreApplication().startSpringApplication(args);
        }
    }

    private static void expandIPv6(final String host) {
        try {
            final String stripBrackets = host.replace("[", "").replace("]", "");
            final InetAddress inetAddress = InetAddress.getByName(stripBrackets);
            if (inetAddress instanceof Inet6Address) {
                System.out.println("[" + inetAddress.toString().substring(1) + "]"); //NOSONAR
            } else {
                //IPv4
                System.out.println(inetAddress.toString().substring(1)); //NOSONAR
            }
        } catch (final UnknownHostException e) {
            //Fail to host string - in case it is an actual hostname
            System.out.println(host); //NOSONAR
        }
    }

    /**
     * Starts backup and restore rest application.
     *
     * @param args
     *            not used.
     */
    public void startSpringApplication(final String[] args) {
        final SpringApplication app = new SpringApplication(BackupAndRestoreApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

    /**
     * Sets version of backup and restore rest application.
     *
     * @param version
     *            to retrieve version of backup and restore rest application from application.properties.
     */
    @Value("${bro.service.version:version_placeholder}")
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * Logs version of backup and restore rest application.
     */
    @PostConstruct
    public void logVersion() {
        if (log.isInfoEnabled()) {
            log.info("Starting Backup and Restore Orchestrator Version: {}", version);
        }
    }

    /**
     * Sets timezone of backup and restore rest application.
     */
    @PostConstruct
    public void setTimezone() {
        final String timeZone;
        final Set<String> zoneIds = ZoneId.getAvailableZoneIds();
        final String tz1 = System.getenv("TZ") == null ? "UTC" : System.getenv("TZ");
        final String tz2 = zoneIds.stream().filter(tz1::equals).findAny().orElse(null);
        if (tz2 != null) {
            timeZone = tz2;
            log.info(tz1 + " TimeZone being used");
        } else if (ZoneId.SHORT_IDS.containsKey(tz1)) {
            timeZone = ZoneId.SHORT_IDS.get(tz1);
            log.info(tz1 + " TimeZone being used");
        } else {
            log.warn("Provided TimeZone ( " + tz1 + " ) is not valid! Using default value (UTC)");
            timeZone = "UTC";
        }
        System.setProperty("user.timezone", timeZone);
    }

}
