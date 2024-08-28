/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.adp.mgmt.bro.api.agent;

import java.io.File;
import java.io.IOException;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.ericsson.adp.mgmt.bro.api.util.CertificateType;
import com.ericsson.adp.mgmt.bro.api.util.RefreshingSslContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.bro.api.exception.InvalidOrchestratorConnectionInformationException;
import com.ericsson.adp.mgmt.bro.api.registration.RegistrationInformation;

import io.grpc.ManagedChannel;
import io.grpc.internal.AbstractManagedChannelImplBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;

/**
 * Responsible for creating agents.
 */
public class AgentFactory {

    private static final Logger log = LogManager.getLogger(AgentFactory.class);

    /**
     * Prevents external instantiation.
     */
    private AgentFactory() {
    }

    /**
     * Creates agent without tls, establishing connection to orchestrator and registering it.
     *
     * @param host address of orchestrator.
     * @param port address of orchestrator.
     * @param agentBehavior specific agent behavior.
     * @return registered agent.
     * @deprecated deprecated to support tls, achieved using createAgent passing in OrchestratorConnectionInformation
     */
    @Deprecated
    public static Agent createAgent(final String host, final int port, final AgentBehavior agentBehavior) {
        final OrchestratorConnectionInformation orchestratorConnectionInformation = new OrchestratorConnectionInformation(host, port);

        return createAgent(orchestratorConnectionInformation, agentBehavior);
    }

    /**
     * Creates agent, establishing connection to orchestrator and registering it.
     * Orchestrator connection information is passed to enable/disable tls and provide security file paths if enabled.
     *
     * @param orchestratorConnectionInformation orchestrator connection information for host, port and to enable/disable tls
     * @param agentBehavior specific agent behavior.
     * @return registered agent.
     */
    public static Agent createAgent(final OrchestratorConnectionInformation orchestratorConnectionInformation, final AgentBehavior agentBehavior) {
        log.info("Creating agent to communicate with orchestrator at host <{}> and port <{}>",
                orchestratorConnectionInformation.getHost(), orchestratorConnectionInformation.getPort());

        final RegistrationInformation registrationInformation = agentBehavior.getRegistrationInformation();
        registrationInformation.validate();

        final Agent agent = new Agent(
                agentBehavior,
                new OrchestratorGrpcChannel(getChannelToOrchestrator(orchestratorConnectionInformation)));
        final int numSecondsToWaitForACK = orchestratorConnectionInformation.getNumberSecondsWaitForACK();
        log.info("The Agent will wait for <{}> seconds for an acknowledgment from the orchestrator", numSecondsToWaitForACK);
        agent.setSecondsToRetryACK(numSecondsToWaitForACK);

        agent.register(new OrchestratorStreamObserver(agent));

        return agent;
    }

    /**
     * Create a channel to the orchestrator
     * @param orchestratorConnectionInformation - definition for channel to orchestrator
     * @return a managed channel to the orchestrator
     * */
    public static ManagedChannel getChannelToOrchestrator(final OrchestratorConnectionInformation orchestratorConnectionInformation) {
        final NettyChannelBuilder channelBuilder = NettyChannelBuilder
                .forAddress(orchestratorConnectionInformation.getHost(), orchestratorConnectionInformation.getPort())
                .maxInboundMessageSize(orchestratorConnectionInformation.getMaxInboundMessageSize())
                .keepAliveTime(2, TimeUnit.MINUTES)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true);

        return applySSL(channelBuilder, orchestratorConnectionInformation).build();
    }

    private static AbstractManagedChannelImplBuilder<NettyChannelBuilder> applySSL(
        final NettyChannelBuilder channelBuilder, final OrchestratorConnectionInformation orchestratorConnectionInformation) {
        if (orchestratorConnectionInformation.isTlsEnabled()) {
            final RefreshingSslContext context;

            try {
                final Map<String, CertificateType> originalCertificatePaths = orchestratorConnectionInformation.getOriginalCertificatePaths();
                for (final String path : originalCertificatePaths.keySet()) {
                    final Optional<String> selectedPath = CertificateHandler.checkCertsInPathAndTakePreference(orchestratorConnectionInformation,
                            path, originalCertificatePaths.get(path));
                    log.info("Selected path {}, {}", selectedPath.get(), originalCertificatePaths.get(path));
                }
                final CertWatcher watcher = new CertWatcher(
                        orchestratorConnectionInformation,
                        5, TimeUnit.SECONDS
                );
                context = new RefreshingSslContext(() -> getSslContextBuilder(orchestratorConnectionInformation), watcher);
            } catch (IOException e) {
                throw new InvalidOrchestratorConnectionInformationException("Failed to build SSL context: ", e);
            }
            return channelBuilder.overrideAuthority(orchestratorConnectionInformation.getCertificateAuthorityName())
                    .sslContext(context);
        }
        log.info("Agent has TLS disabled");
        return channelBuilder.usePlaintext();
    }

    private static SslContextBuilder getSslContextBuilder(final OrchestratorConnectionInformation information) {
        final SslContextBuilder builder = GrpcSslContexts.forClient();
        if (information.getCertificateAuthorityPath() != null) {
            builder.trustManager(new File(information.getCertificateAuthorityPath()));
            log.info("Agent has TLS enabled");
        }
        if (information.getClientCertificatePath() != null) {
            builder.keyManager(new File(information.getClientCertificatePath()),
                new File(information.getClientPrivKeyPath()));
            log.info("Agent has mTLS enabled");
        } else {
            log.info("mTLS disabled on the agent");
        }
        return builder;
    }

}
