package se.crisp.codekvast.server.agent.impl;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import se.crisp.codekvast.server.agent.AgentRestEndpoints;
import se.crisp.codekvast.server.agent.ServerDelegate;
import se.crisp.codekvast.server.agent.ServerDelegateConfig;
import se.crisp.codekvast.server.agent.ServerDelegateException;
import se.crisp.codekvast.server.agent.model.test.Ping;
import se.crisp.codekvast.server.agent.model.test.Pong;
import se.crisp.codekvast.server.agent.model.v1.*;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Set;

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
    private final Validator validator;

    @Getter
    private final RestTemplate restTemplate;

    @Inject
    public ServerDelegateImpl(ServerDelegateConfig config, Validator validator) {
        this.config = config;
        this.validator = validator;
        this.header = Header.builder().customerName(config.getCustomerName()).environment(config.getEnvironment()).build();
        this.restTemplate = new RestTemplate(createBasicAuthHttpClient(config.getApiAccessID(), config.getApiAccessSecret()));
    }

    HttpComponentsClientHttpRequestFactory createBasicAuthHttpClient(final String username, final String password) {
        CredentialsProvider cp = new BasicCredentialsProvider();
        cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        return new HttpComponentsClientHttpRequestFactory(HttpClients.custom().setDefaultCredentialsProvider(cp).build());
    }

    @Override
    public void uploadJvmData(String appName, String appVersion, String hostName, long startedAtMillis, long dumpedAtMillis,
                              String jvmFingerprint, String codekvastVersion, String codekvastVcsId)
            throws ServerDelegateException {
        String endPoint = config.getServerUri() + AgentRestEndpoints.UPLOAD_V1_JVM_RUN;

        log.debug("Uploading JVM run data to {}", endPoint);

        try {
            long startedAt = System.currentTimeMillis();

            JvmData data = JvmData.builder().header(header)
                                        .appName(appName)
                                        .appVersion(appVersion)
                                        .hostName(hostName)
                                        .startedAtMillis(startedAtMillis)
                                        .dumpedAtMillis(dumpedAtMillis)
                                        .jvmFingerprint(jvmFingerprint)
                                        .codekvastVersion(codekvastVersion)
                                        .codekvastVcsId(codekvastVcsId)
                                        .build();

            restTemplate.postForEntity(new URI(endPoint), validate(data), Void.class);

            log.info("Uploaded {} to {} in {}s", data, endPoint, elapsedSeconds(startedAt));
        } catch (URISyntaxException e) {
            throw new ServerDelegateException("Illegal REST endpoint: " + endPoint, e);
        } catch (RestClientException e) {
            throw new ServerDelegateException("Failed to post JVM run data", e);
        }
    }

    @Override
    public void uploadSignatureData(String jvmFingerprint, Collection<String> signatures) throws ServerDelegateException {
        if (signatures.isEmpty()) {
            log.debug("Not uploading empty signatures");
            return;
        }

        String endPoint = config.getServerUri() + AgentRestEndpoints.UPLOAD_V1_SIGNATURES;
        log.debug("Uploading {} signatures to {}", signatures.size(), endPoint);

        try {
            long startedAtMillis = System.currentTimeMillis();

            SignatureData data = SignatureData.builder().header(header).jvmFingerprint(jvmFingerprint).signatures(signatures).build();

            restTemplate.postForEntity(new URI(endPoint), validate(data), Void.class);

            log.info("Uploaded {} signatures to {} in {}s", signatures.size(), endPoint, elapsedSeconds(startedAtMillis));
        } catch (URISyntaxException e) {
            throw new ServerDelegateException("Illegal REST endpoint: " + endPoint, e);
        } catch (RestClientException e) {
            throw new ServerDelegateException("Failed to post signature data", e);
        }
    }

    @Override
    public void uploadInvocationsData(@NonNull String jvmFingerprint, Collection<InvocationEntry> invocations)
            throws ServerDelegateException {
        if (invocations.isEmpty()) {
            log.debug("Not uploading empty invocations");
            return;
        }

        String endPoint = config.getServerUri() + AgentRestEndpoints.UPLOAD_V1_INVOCATIONS;
        log.debug("Uploading {} signatures to {}", invocations.size(), endPoint);

        try {
            long startedAtMillis = System.currentTimeMillis();

            InvocationData data = InvocationData.builder().header(header).jvmFingerprint(jvmFingerprint).invocations(invocations).build();

            restTemplate.postForEntity(new URI(endPoint), validate(data), Void.class);

            log.info("Uploaded {} signatures to {} in {}s", invocations.size(), endPoint, elapsedSeconds(startedAtMillis));
        } catch (URISyntaxException e) {
            throw new ServerDelegateException("Illegal REST endpoint: " + endPoint, e);
        } catch (RestClientException e) {
            throw new ServerDelegateException("Failed to post invocations data", e);
        }
    }

    @Override
    public String ping(String message) throws ServerDelegateException {
        String endPoint = config.getServerUri() + AgentRestEndpoints.PING;
        log.debug("Sending ping '{}' to {}", message, endPoint);
        try {

            Ping data = Ping.builder().message(message).build();

            ResponseEntity<Pong> response = restTemplate.postForEntity(new URI(endPoint), data, Pong.class);

            String pongMessage = response.getBody().getMessage();
            log.debug("Server responded with '{}'", pongMessage);
            return pongMessage;
        } catch (URISyntaxException e) {
            throw new ServerDelegateException("Illegal REST endpoint: " + endPoint, e);
        } catch (RestClientException e) {
            throw new ServerDelegateException("Failed to post invocations data", e);
        }
    }

    private <T> T validate(T data) throws ServerDelegateException {
        Set<ConstraintViolation<T>> violations = validator.validate(data);

        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid ").append(data.getClass().getSimpleName());
            String delimiter = ": ";
            for (ConstraintViolation<T> violation : violations) {
                sb.append(delimiter).append(violation.getPropertyPath()).append(" ").append(violation.getInvalidValue()).append(" ").append
                        (violation.getMessage());
                delimiter = ", ";
            }

            throw new ServerDelegateException(sb.toString());
        }

        return data;
    }

    private int elapsedSeconds(long startedAtMillis) {
        return Math.round((System.currentTimeMillis() - startedAtMillis) / 1000f);
    }

}
