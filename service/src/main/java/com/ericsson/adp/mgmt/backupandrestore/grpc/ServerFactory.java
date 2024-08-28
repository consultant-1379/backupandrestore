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
package com.ericsson.adp.mgmt.backupandrestore.grpc;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextService;

/**
 * Responsible for creating GRPC server.
 */
@Configuration
public class ServerFactory {

    private static final Logger log = LogManager.getLogger(ServerFactory.class);

    private int port;
    private boolean tlsEnabled;
    private ControlInterfaceImplementation controlInterface;
    private DataInterfaceImplementation dataInterface;
    private SslContextService sslContextService;

    @Bean
    public Server getServer() {
        return createServer();
    }

    private Server createServer() {
        try {
            log.info("Creating grpc server at port {}", port);
            final NettyServerBuilder serverBuilder = NettyServerBuilder
                    .forPort(port)
                    .addService(controlInterface)
                    .addService(dataInterface)
                    .keepAliveTime(2, TimeUnit.MINUTES)
                    .keepAliveTimeout(10, TimeUnit.SECONDS)
                    .permitKeepAliveTime(100, TimeUnit.SECONDS)
                    .permitKeepAliveWithoutCalls(true);
            return applySecurity(serverBuilder).build().start();
        } catch (final Exception e) {
            log.error("Exception starting grpc server", e);
            throw new BackupServiceException("Exception starting grpc server", e);
        }
    }

    private NettyServerBuilder applySecurity(final NettyServerBuilder serverBuilder) {
        if (tlsEnabled) {
            log.info("GRPC server has TLS enabled");
            return serverBuilder
                    .sslContext(sslContextService.getSslContext());
        }
        log.info("GRPC server has TLS disabled");
        return serverBuilder;
    }

    @Value("${grpc.server.port}")
    public void setPort(final int port) {
        this.port = port;
    }

    @Value("${flag.global.security:true}")
    public void setTlsEnabled(final boolean isTlsEnabled) {
        this.tlsEnabled = isTlsEnabled;
    }

    @Autowired
    public void setControlInterface(final ControlInterfaceImplementation controlInterface) {
        this.controlInterface = controlInterface;
    }

    @Autowired
    public void setDataInterface(final DataInterfaceImplementation dataInterface) {
        this.dataInterface = dataInterface;
    }

    @Autowired
    public void setSslContextService(final SslContextService sslContextService) {
        this.sslContextService = sslContextService;
    }

}
