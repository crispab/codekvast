package se.crisp.codekvast.server.agent_api.impl;

import lombok.Getter;
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
import se.crisp.codekvast.server.agent_api.AgentApi;
import se.crisp.codekvast.server.agent_api.AgentApiConfig;
import se.crisp.codekvast.server.agent_api.AgentApiException;
import se.crisp.codekvast.server.agent_api.AgentRestEndpoints;
import se.crisp.codekvast.server.agent_api.model.test.Ping;
import se.crisp.codekvast.server.agent_api.model.test.Pong;
import se.crisp.codekvast.server.agent_api.model.v1.InvocationData;
import se.crisp.codekvast.server.agent_api.model.v1.InvocationEntry;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The implementation of the AgentApi.
 *
 * Uses a Spring RestTemplate for doing the REST calls.
 *
 * @author Olle Hallin
 */
@Slf4j
@Component
public class AgentApiImpl implements AgentApi {

    private static final int UPLOAD_CHUNK_SIZE = 1000;

    private final AgentApiConfig config;
    private final Validator validator;

    @Getter
    private final RestTemplate restTemplate;

    @Inject
    public AgentApiImpl(AgentApiConfig config, Validator validator) {
        this.config = config;
        this.validator = validator;
        this.restTemplate = new RestTemplate(createBasicAuthHttpClient(config.getApiAccessID(), config.getApiAccessSecret()));
    }

    HttpComponentsClientHttpRequestFactory createBasicAuthHttpClient(final String username, final String password) {
        CredentialsProvider cp = new BasicCredentialsProvider();
        cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        return new HttpComponentsClientHttpRequestFactory(HttpClients.custom().setDefaultCredentialsProvider(cp).build());
    }

    @Override
    public void uploadJvmData(JvmData jvmData) throws AgentApiException {
        String endPoint = config.getServerUri() + AgentRestEndpoints.UPLOAD_V1_JVM_RUN;
        log.debug("Uploading JVM data to {}", endPoint);

        try {
            long startedAt = System.currentTimeMillis();

            restTemplate.postForEntity(new URI(endPoint), validate(jvmData), Void.class);

            log.info("Uploaded JVM data from {} to {} in {}s", jvmData.getAppName(), endPoint, elapsedSeconds(startedAt));
        } catch (URISyntaxException e) {
            throw new AgentApiException("Illegal REST endpoint: " + endPoint, e);
        } catch (RestClientException e) {
            throw new AgentApiException("Failed to post JVM run data", e);
        }
    }

    @Override
    public void uploadSignatureData(JvmData jvmData, Collection<String> signatures) throws AgentApiException {
        if (signatures.isEmpty()) {
            log.debug("Not uploading empty signatures");
            return;
        }

        List<InvocationEntry> invocations = new ArrayList<>(signatures.size());
        for (String s : signatures) {
            invocations.add(new InvocationEntry(s, 0L, null));
        }

        uploadInvocationsData(jvmData, invocations);
    }

    @Override
    public void uploadInvocationsData(JvmData jvmData, Collection<InvocationEntry> invocations)
            throws AgentApiException {
        if (invocations.isEmpty()) {
            log.debug("Not uploading empty invocations");
            return;
        }

        String endPoint = config.getServerUri() + AgentRestEndpoints.UPLOAD_V1_INVOCATIONS;
        log.debug("Uploading {} signatures from {} to {}", invocations.size(), jvmData.getAppName(), endPoint);
        long startedAtMillis = System.currentTimeMillis();

        try {
            URI uri = new URI(endPoint);

            List<InvocationEntry> list = new ArrayList<>(invocations);
            int from = 0;
            int chunkNo = 1;
            int uploaded = 0;
            while (from < list.size()) {
                int to = Math.min(from + UPLOAD_CHUNK_SIZE, list.size());

                uploaded += uploadInvocationChunk(jvmData, uri, list.subList(from, to), chunkNo);

                from = to;
                chunkNo += 1;
            }

            checkState(uploaded == invocations.size(), "Bad chunk logic: uploaded=" + uploaded + ", input.size()=" + invocations.size());
            log.info("Uploaded {} invocations from {} to {} in {}s", uploaded, jvmData.getAppName(), endPoint,
                     elapsedSeconds(startedAtMillis));
        } catch (URISyntaxException e) {
            throw new AgentApiException("Illegal REST endpoint: " + endPoint, e);
        }
    }

    private void checkState(boolean b, String message) {
        if (!b) {
            throw new IllegalStateException(message);
        }
    }

    private int uploadInvocationChunk(JvmData jvmData, URI uri, List<InvocationEntry> chunk, int chunkNo) throws AgentApiException {
        try {
            long startedAt = System.currentTimeMillis();
            log.debug("Uploading chunk #{} of size {}", chunkNo, chunk.size());
            InvocationData data = InvocationData.builder().jvmFingerprint(jvmData.getJvmFingerprint()).invocations(chunk).build();
            restTemplate.postForEntity(uri, validate(data), Void.class);
            log.debug("Uploaded chunk #{} in {} ms", chunkNo, System.currentTimeMillis() - startedAt);
            return chunk.size();
        } catch (RestClientException e) {
            throw new AgentApiException("Failed to post invocations data", e);
        }
    }

    @Override
    public String ping(String message) throws AgentApiException {
        String endPoint = config.getServerUri() + AgentRestEndpoints.PING;
        log.debug("Sending ping '{}' to {}", message, endPoint);
        try {
            Ping data = Ping.builder().message(message).build();

            ResponseEntity<Pong> response = restTemplate.postForEntity(new URI(endPoint), data, Pong.class);

            String pongMessage = response.getBody().getMessage();
            log.debug("Server responded with '{}'", pongMessage);
            return pongMessage;
        } catch (URISyntaxException e) {
            throw new AgentApiException("Illegal REST endpoint: " + endPoint, e);
        } catch (RestClientException e) {
            throw new AgentApiException("Failed to post invocations data", e);
        }
    }

    @Override
    public URI getServerUri() {
        return config.getServerUri();
    }

    private <T> T validate(T data) throws AgentApiException {
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

            throw new AgentApiException(sb.toString());
        }

        return data;
    }

    private int elapsedSeconds(long startedAtMillis) {
        return Math.round((System.currentTimeMillis() - startedAtMillis) / 1000f);
    }

}
