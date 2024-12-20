package ca.uhn.fhir.jpa.starter.common;

import javax.net.ssl.SSLContext;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.util.ITestingUiClientFactory;
import ca.uhn.fhir.to.FhirTesterMvcConfig;
import ca.uhn.fhir.to.TesterConfig;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
@Import(FhirTesterMvcConfig.class)
public class FhirTesterConfig {
	private static final Logger logger = LoggerFactory.getLogger(FhirTesterConfig.class);

	@Value("${hapi.fhir.tester.home.name:Local Tester}")
	private String name;
	@Value("${hapi.fhir.tester.home.server_address:https://localhost:8443/fhir}")
	private String serverAddress;
	@Value("${hapi.fhir.tester.home.refuse_to_fetch_third_party_urls:false}")
	private boolean refuseToFetchThirdPartyUrls;
	@Value("${hapi.fhir.tester.home.fhir_version:R4}")
	private String fhirVersion;

	@Value("${spring.config.oauth2.auth-server-url}")
	private String authServerUrl;
	@Value("${spring.config.oauth2.client-id}")
	private String clientId;
	@Value("${spring.config.oauth2.client-secret}")
	private String clientSecret;

	private final SSLContext sslContext;

	public FhirTesterConfig(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	private String getAccessToken() {
        try {
            logger.info("Attempting to get access token from {}", authServerUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("grant_type", "client_credentials");
            map.add("client_id", clientId);
            map.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            RestTemplate restTemplate = new RestTemplate();
            String tokenUrl = authServerUrl + "/protocol/openid-connect/token";
            
            logger.debug("Token request URL: {}", tokenUrl);
            logger.debug("Token request body: {}", map);

            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                    tokenUrl,
                    request,
                    TokenResponse.class);

            if (response.getBody() != null) {
                logger.debug("Raw token response: {}", response.getBody());
                if (response.getBody().getAccessToken() != null) {
                    logger.info("Successfully obtained access token");
                    return response.getBody().getAccessToken();
                } else {
                    logger.error("Received null access token from OAuth server");
                    throw new RuntimeException("Failed to obtain access token - null access token");
                }
            } else {
                logger.error("Received null response from OAuth server");
                throw new RuntimeException("Failed to obtain access token - null response");
            }
        } catch (Exception e) {
            logger.error("Failed to obtain access token", e);
            throw new RuntimeException("Failed to obtain access token: " + e.getMessage(), e);
        }
	}

	@Bean
	public TesterConfig testerConfig() {
		TesterConfig retVal = new TesterConfig();
		
		retVal.addServer()
			.withId("home")
			.withFhirVersion(FhirVersionEnum.valueOf(fhirVersion))
			.withBaseUrl(serverAddress)
			.withName(name);

		retVal.setClientFactory(new ITestingUiClientFactory() {
			@Override
			public IGenericClient newClient(FhirContext theFhirContext, HttpServletRequest theRequest, String theServerBase) {
                logger.info("Creating new FHIR client for server: {}", theServerBase);
                
				theFhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
				theFhirContext.getRestfulClientFactory().setSocketTimeout(5 * 60 * 1000);
				
				CloseableHttpClient httpClient = HttpClients.custom()
						.setSSLContext(sslContext)
						.build();
				
				theFhirContext.getRestfulClientFactory().setHttpClient(httpClient);
				
				IGenericClient client = theFhirContext.newRestfulGenericClient(theServerBase);
				
				String token = getAccessToken();
                logger.debug("Adding bearer token to request headers");
				client.registerInterceptor(new BearerTokenAuthInterceptor(token));
				
				return client;
			}
		});

		return retVal;
	}

	private static class TokenResponse {
		@JsonProperty("access_token")
		private String accessToken;
		
		@JsonProperty("expires_in")
		private Integer expiresIn;
		
		@JsonProperty("refresh_expires_in")
		private Integer refreshExpiresIn;
		
		@JsonProperty("token_type")
		private String tokenType;
		
		@JsonProperty("not-before-policy")
		private Integer notBeforePolicy;
		
		@JsonProperty("scope")
		private String scope;

		public String getAccessToken() {
			return accessToken;
		}

		public void setAccessToken(String accessToken) {
			this.accessToken = accessToken;
		}

		public Integer getExpiresIn() {
			return expiresIn;
		}

		public void setExpiresIn(Integer expiresIn) {
			this.expiresIn = expiresIn;
		}

		public Integer getRefreshExpiresIn() {
			return refreshExpiresIn;
		}

		public void setRefreshExpiresIn(Integer refreshExpiresIn) {
			this.refreshExpiresIn = refreshExpiresIn;
		}

		public String getTokenType() {
			return tokenType;
		}

		public void setTokenType(String tokenType) {
			this.tokenType = tokenType;
		}

		public Integer getNotBeforePolicy() {
			return notBeforePolicy;
		}

		public void setNotBeforePolicy(Integer notBeforePolicy) {
			this.notBeforePolicy = notBeforePolicy;
		}

		public String getScope() {
			return scope;
		}

		public void setScope(String scope) {
			this.scope = scope;
		}

		@Override
		public String toString() {
			return "TokenResponse{" +
					"accessToken='" + (accessToken != null ? accessToken.substring(0, 20) + "..." : "null") + '\'' +
					", expiresIn=" + expiresIn +
					", refreshExpiresIn=" + refreshExpiresIn +
					", tokenType='" + tokenType + '\'' +
					", notBeforePolicy=" + notBeforePolicy +
					", scope='" + scope + '\'' +
					'}';
		}
	}
}
