package dev.tomic.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@Service
public class KeycloakAuthenticator {
    @Value("${app.auth-url}")
    private String authUrl;
    @Value("${app.grant-type}")
    private String grantType;
    @Value("${app.client-id}")
    private String clientId;
    @Value("${app.client-secret}")
    private String clientSecret;
    @Value("${app.scope}")
    private String scope;

    private final ObjectMapper objectMapper;
    Logger logger = LoggerFactory.getLogger(KeycloakAuthenticator.class);
    private AuthObject credentials;

    private AuthRequestObject authRequestObject;

    public AuthObject getCredentials() {
        return credentials;
    }

    public KeycloakAuthenticator() {
        this.objectMapper = new ObjectMapper();
    }

    private HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    @Scheduled(fixedDelayString = "${app.token-lifespan}", timeUnit = TimeUnit.MINUTES)
    private void authenticate() throws IOException, URISyntaxException, InterruptedException {
        if (credentials == null) {
            credentials = retrieveToken();
            return;
        }
        credentials = refreshToken();
    }

    private AuthObject retrieveToken() throws IOException, InterruptedException, URISyntaxException {
        this.authRequestObject = new AuthRequestObject(grantType, clientId, clientSecret, scope, null);

        String requestBody = encodeRetrieveTokenFormData(authRequestObject);
        HttpRequest request = createBasicHttpRequest(requestBody);
        HttpResponse<String> response = createHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == HttpStatus.OK.value()) {
            logger.info(() -> "Token retrieved successfully.");
            return objectMapper.readValue(response.body(), AuthObject.class);
        }

        throw new AuthenticationException(response.body());
    }

    private AuthObject refreshToken() throws URISyntaxException, IOException, InterruptedException {
        this.authRequestObject = new AuthRequestObject(grantType, clientId, clientSecret, scope, credentials.getRefreshToken());

        String requestBody = encodeRefreshTokenFormData(authRequestObject);
        HttpRequest request = createBasicHttpRequest(requestBody);
        HttpResponse<String> response = createHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == HttpStatus.OK.value()) {
            logger.info(() -> "Token refreshed successfully.");
            return objectMapper.readValue(response.body(), AuthObject.class);
        } else {
            return retrieveToken();
        }
    }

    private String encodeRetrieveTokenFormData(AuthRequestObject authRequestObject) {
        return "grant_type=" + URLEncoder.encode(authRequestObject.getGrantType(), StandardCharsets.UTF_8) +
                "&client_id=" + URLEncoder.encode(authRequestObject.getClientId(), StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(authRequestObject.getClientSecret(), StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(authRequestObject.getScope(), StandardCharsets.UTF_8);
    }

    private String encodeRefreshTokenFormData(AuthRequestObject authRequestObject) {
        return "grant_type=" + URLEncoder.encode("refresh_token", StandardCharsets.UTF_8) +
                "&client_id=" + URLEncoder.encode(authRequestObject.getClientId(), StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(authRequestObject.getClientSecret(), StandardCharsets.UTF_8) +
                "&refresh_token=" + URLEncoder.encode(authRequestObject.getRefreshToken(), StandardCharsets.UTF_8);
    }

    private HttpRequest createBasicHttpRequest(String body) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(authUrl))
                .header(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }
}
