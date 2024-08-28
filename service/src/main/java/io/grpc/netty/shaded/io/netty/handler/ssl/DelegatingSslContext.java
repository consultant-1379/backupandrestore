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
package io.grpc.netty.shaded.io.netty.handler.ssl;

import java.util.List;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSessionContext;

import io.grpc.netty.shaded.io.netty.buffer.ByteBufAllocator;

/**
 * Responsible for updating SslContext containing Server Certificate and
 * private key, the SslContext is used within the GRPC Server.
 */
public class DelegatingSslContext extends SslContext {

    private SslContext ctx;
    private SslContextBuilder ctxBuilder;

    /**
     * Creates a DelegatingSslContext with a SslContextBuilder.
     * @param builder the SslContextBuilder used by the GRPC Server.
     * @throws SSLException
     *             Thrown if the SslContextBuilder fails to build
     */
    public DelegatingSslContext(final SslContextBuilder builder) throws SSLException {
        this.ctx = builder.build();
        this.ctxBuilder = builder;
    }

    @Override
    public final boolean isClient() {
        return ctx.isClient();
    }

    @Override
    public final List<String> cipherSuites() {
        return ctx.cipherSuites();
    }

    @Override
    public final long sessionCacheSize() {
        return ctx.sessionCacheSize();
    }

    @Override
    public final long sessionTimeout() {
        return ctx.sessionTimeout();
    }

    /**
     * @deprecated in ssl
     */
    @Deprecated
    @Override
    public final ApplicationProtocolNegotiator applicationProtocolNegotiator() {
        return ctx.applicationProtocolNegotiator();
    }

    @Override
    public final SSLEngine newEngine(final ByteBufAllocator alloc) {
        return ctx.newEngine(alloc);
    }

    @Override
    public final SSLEngine newEngine(final ByteBufAllocator alloc, final String peerHost, final int peerPort) {
        return ctx.newEngine(alloc, peerHost, peerPort);
    }

    @Override
    protected final SslHandler newHandler(final ByteBufAllocator alloc, final boolean startTls) {
        return ctx.newHandler(alloc, startTls);
    }

    @Override
    protected final SslHandler newHandler(final ByteBufAllocator alloc, final String peerHost, final int peerPort,
            final boolean startTls) {
        return ctx.newHandler(alloc, peerHost, peerPort, startTls);
    }

    @Override
    public final SSLSessionContext sessionContext() {
        return ctx.sessionContext();
    }

    /**
     * Updates the SslContextBuilder used by the GRPC Server.
     * @param sslContextBuilder sslContextBuilder to update
     * @throws SSLException
     *             Thrown if the SslContextBuilder fails to build
     */
    public synchronized void update(final SslContextBuilder sslContextBuilder) throws SSLException {
        ctxBuilder = sslContextBuilder;
        ctx = ctxBuilder.build();
    }

}
