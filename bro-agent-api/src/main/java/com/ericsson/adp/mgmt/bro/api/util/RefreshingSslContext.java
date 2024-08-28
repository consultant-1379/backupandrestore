/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.bro.api.util;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSessionContext;

import com.ericsson.adp.mgmt.bro.api.agent.CertWatcher;
import com.ericsson.adp.mgmt.bro.api.agent.OrchestratorConnectionInformation;
import io.grpc.netty.shaded.io.netty.buffer.ByteBufAllocator;
import io.grpc.netty.shaded.io.netty.handler.ssl.ApplicationProtocolNegotiator;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;

/**
 * Responsible for updating SslContext containing Server Certificate and
 * private key, the SslContext is used within the GRPC Server.
 */
public class RefreshingSslContext extends SslContext {
    private final AtomicReference<SslContext> ctx;

    private final CertWatcher watcher;

    /**
     * Creates a DelegatingSslContext with a SslContextBuilder.
     * @param builderSupplier a supplier for the SslContextBuilder used by the GRPC Server.
     * @param watcher - the CertWatcher this RefreshingSslContext will use to handle refresh on certificate re-issue.
     * @throws SSLException
     *             Thrown if the SslContextBuilder fails to build
     */
    public RefreshingSslContext(final Supplier<SslContextBuilder> builderSupplier, final CertWatcher watcher) throws SSLException {
        this.ctx = new AtomicReference<>(builderSupplier.get().build());
        this.watcher = watcher;
        watcher.addCallback(() -> {
            try {
                final SslContextBuilder builder = builderSupplier.get();
                final OrchestratorConnectionInformation information = watcher.getOrchestratorConnectionInformation();
                if (information.getCertificateAuthorityPath() != null) {
                    builder.trustManager(new File(information.getCertificateAuthorityPath()));
                }
                if (information.getClientCertificatePath() != null) {
                    builder.keyManager(new File(information.getClientCertificatePath()),
                            new File(information.getClientPrivKeyPath()));
                }
                ctx.set(builder.build());
            } catch (SSLException e) {
                return Optional.of(e);
            }
            return Optional.empty();
        });
        watcher.updateLastRead();
        watcher.start();
    }

    @Override
    public final boolean isClient() {
        return ctx.get().isClient();
    }

    @Override
    public final List<String> cipherSuites() {
        return ctx.get().cipherSuites();
    }

    @Override
    public final long sessionCacheSize() {
        return ctx.get().sessionCacheSize();
    }

    @Override
    public final long sessionTimeout() {
        return ctx.get().sessionTimeout();
    }

    /**
     * @deprecated in ssl
     */
    @Deprecated
    @Override
    public final ApplicationProtocolNegotiator applicationProtocolNegotiator() {
        return ctx.get().applicationProtocolNegotiator();
    }

    @Override
    public final SSLEngine newEngine(final ByteBufAllocator alloc) {
        return ctx.get().newEngine(alloc);
    }

    @Override
    public final SSLEngine newEngine(final ByteBufAllocator alloc, final String peerHost, final int peerPort) {
        return ctx.get().newEngine(alloc, peerHost, peerPort);
    }

    @Override
    public final SSLSessionContext sessionContext() {
        return ctx.get().sessionContext();
    }

    /**
     * Get the underlying cert watcher, for example, to ::stop it before dropping the context
     * @return the underlying cert watcher
     * */
    public CertWatcher getWatcher() {
        return watcher; // This exists because PMD mandates it
    }
}
