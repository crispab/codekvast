package se.crisp.codekvast.server.agent.impl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import se.crisp.codekvast.server.agent.AgentRestEndpoints;
import se.crisp.codekvast.server.agent.ServerDelegate;
import se.crisp.codekvast.server.agent.ServerDelegateConfig;
import se.crisp.codekvast.server.agent.ServerDelegateException;
import se.crisp.codekvast.server.agent.model.v1.*;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.UUID;

/**
 * The implementation of the ServerDelegate.
 * <p/>
 * Uses a Spring RestTemplate for doing the REST calls.
 *
 * @author Olle Hallin
 */
@Slf4j
@Component
public class ServerDelegateImpl implements ServerDelegate {

    private final ServerDelegateConfig config;
    private final Header header;

    @Getter
    private final RestTemplate restTemplate;

    @Inject
    public ServerDelegateImpl(ServerDelegateConfig config) {
        this.config = config;
        this.header = Header.builder()
                            .customerName(config.getCustomerName())
                            .appName(config.getAppName())
                            .appVersion(config.getAppVersion())
                            .environment(config.getEnvironment())
                            .codeBaseName(config.getCodeBaseName())
                            .build();
        this.restTemplate = new RestTemplate(createBasicAuthHttpClient(config.getApiUsername(), config.getApiPassword()));
    }

    HttpComponentsClientHttpRequestFactory createBasicAuthHttpClient(final String username, final String password) {
        CredentialsProvider cp = new BasicCredentialsProvider();
        cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        return new HttpComponentsClientHttpRequestFactory(HttpClients.custom().setDefaultCredentialsProvider(cp).build());
    }

    @Override
    public void uploadJvmRunData(String hostName, long startedAtMillis, long dumpedAtMillis, UUID uuid) throws ServerDelegateException {
        String endPoint = config.getServerUri() + AgentRestEndpoints.UPLOAD_JVM_RUN_V1;

        log.debug("Uploading sensor run data to {}", endPoint);

        try {
            long startedAt = System.currentTimeMillis();

            JvmRunData data = JvmRunData.builder().header(header)
                                        .hostName(hostName)
                                        .startedAtMillis(startedAtMillis)
                                        .dumpedAtMillis(dumpedAtMillis)
                                        .uuid(uuid)
                                        .build();

            restTemplate.postForLocation(new URI(endPoint), data);

            log.info("Uploaded {} to {} in {} ms", data, endPoint, System.currentTimeMillis() - startedAt);
        } catch (URISyntaxException e) {
            throw new ServerDelegateException("Illegal REST endpoint: " + endPoint, e);
        } catch (RestClientException e) {
            throw new ServerDelegateException("Failed to post sensor data", e);
        }
    }

    @Override
    public void uploadSignatureData(Collection<String> signatures) throws ServerDelegateException {
        if (!signatures.isEmpty()) {
            String endPoint = config.getServerUri() + AgentRestEndpoints.UPLOAD_SIGNATURES_V1;
            log.debug("Uploading {} signatures to {}", signatures.size(), endPoint);

            try {
                long startedAt = System.currentTimeMillis();

                SignatureData data = SignatureData.builder().header(header).signatures(signatures).build();

                restTemplate.postForLocation(new URI(endPoint), data);

                log.info("Uploaded {} signatures to {} in {} ms", signatures.size(), endPoint, System.currentTimeMillis() - startedAt);
            } catch (URISyntaxException e) {
                throw new ServerDelegateException("Illegal REST endpoint: " + endPoint, e);
            } catch (RestClientException e) {
                throw new ServerDelegateException("Failed to post signature data", e);
            }
        }
    }

    @Override
    public void uploadUsageData(Collection<UsageDataEntry> usage) throws ServerDelegateException {
        if (!usage.isEmpty()) {
            String endPoint = config.getServerUri() + AgentRestEndpoints.UPLOAD_USAGE_V1;
            log.debug("Uploading {} signatures to {}", usage.size(), endPoint);

            try {
                long startedAt = System.currentTimeMillis();

                UsageData data = UsageData.builder().header(header).usage(usage).build();

                restTemplate.postForLocation(new URI(endPoint), data);

                log.info("Uploaded {} signatures to {} in {} ms", usage.size(), endPoint, System.currentTimeMillis() - startedAt);
            } catch (URISyntaxException e) {
                throw new ServerDelegateException("Illegal REST endpoint: " + endPoint, e);
            } catch (RestClientException e) {
                throw new ServerDelegateException("Failed to post usage data", e);
            }
        }
    }

}
