package se.crisp.duck.server.agent.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import se.crisp.duck.server.agent.AgentRestEndpoints;
import se.crisp.duck.server.agent.ServerDelegate;
import se.crisp.duck.server.agent.ServerDelegateException;
import se.crisp.duck.server.agent.model.v1.Header;
import se.crisp.duck.server.agent.model.v1.SensorData;
import se.crisp.duck.server.agent.model.v1.SignatureData;
import se.crisp.duck.server.agent.model.v1.UsageData;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * @author Olle Hallin
 */
@Slf4j
@Component
public class ServerDelegateImpl implements ServerDelegate {

    private final ServerDelegate.Config config;
    private final RestTemplate restTemplate;

    @Inject
    public ServerDelegateImpl(Config config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    @Override
    public void uploadSensor(String hostName, long startedAtMillis, long dumpedAtMillis, UUID uuid) throws ServerDelegateException {
        String endPoint = config.getServerUri() + AgentRestEndpoints.UPLOAD_SENSOR_V1;

        log.debug("Uploading sensor data to {}", endPoint);

        try {
            long startedAt = System.currentTimeMillis();

            SensorData data = SensorData.builder().header(buildHeader())
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
    public void uploadSignatures(Collection<String> signatures) throws ServerDelegateException {
        if (!signatures.isEmpty()) {
            String endPoint = config.getServerUri() + AgentRestEndpoints.UPLOAD_SIGNATURES_V1;
            log.debug("Uploading {} signatures to {}", signatures.size(), endPoint);

            try {
                long startedAt = System.currentTimeMillis();

                SignatureData data = SignatureData.builder().header(buildHeader()).signatures(signatures).build();

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
    public void uploadUsage(Map<String, Long> usage) throws ServerDelegateException {
        if (!usage.isEmpty()) {
            String endPoint = config.getServerUri() + AgentRestEndpoints.UPLOAD_USAGE_V1;
            log.debug("Uploading {} signatures to {}", usage.size(), endPoint);

            try {
                long startedAt = System.currentTimeMillis();

                UsageData data = UsageData.builder().header(buildHeader()).usage(usage).build();

                restTemplate.postForLocation(new URI(endPoint), data);

                log.info("Uploaded {} signatures to {} in {} ms", usage.size(), endPoint, System.currentTimeMillis() - startedAt);
            } catch (URISyntaxException e) {
                throw new ServerDelegateException("Illegal REST endpoint: " + endPoint, e);
            } catch (RestClientException e) {
                throw new ServerDelegateException("Failed to post usage data", e);
            }
        }
    }

    private Header buildHeader() {
        return Header.builder()
                     .customerName(config.getCustomerName())
                     .appName(config.getAppName())
                     .environment(config.getEnvironment())
                     .codeBaseName(config.getCodeBaseName())
                     .build();
    }
}
