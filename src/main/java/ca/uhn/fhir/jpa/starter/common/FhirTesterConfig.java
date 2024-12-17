package ca.uhn.fhir.jpa.starter.common;

import javax.net.ssl.SSLContext;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

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

	@Value("${hapi.fhir.tester.home.name:Local Tester}")
	private String name;
	@Value("${hapi.fhir.tester.home.server_address:https://localhost:8443/fhir}")
	private String serverAddress;
	@Value("${hapi.fhir.tester.home.refuse_to_fetch_third_party_urls:false}")
	private boolean refuseToFetchThirdPartyUrls;
	@Value("${hapi.fhir.tester.home.fhir_version:R4}")
	private String fhirVersion;

	private final SSLContext sslContext;

	public FhirTesterConfig(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	@Bean
	public TesterConfig testerConfig() {
		TesterConfig retVal = new TesterConfig();
		
		// Add local server
		retVal.addServer()
			.withId("home")
			.withFhirVersion(FhirVersionEnum.valueOf(fhirVersion))
			.withBaseUrl(serverAddress)
			.withName(name);

		retVal.setClientFactory(new ITestingUiClientFactory() {
			@Override
			public IGenericClient newClient(FhirContext theFhirContext, HttpServletRequest theRequest, String theServerBase) {
				theFhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
				theFhirContext.getRestfulClientFactory().setSocketTimeout(5 * 60 * 1000);
				
				// Create HTTP client with SSL context
				CloseableHttpClient httpClient = HttpClients.custom()
						.setSSLContext(sslContext)
						.build();
				
				theFhirContext.getRestfulClientFactory().setHttpClient(httpClient);
				
				return theFhirContext.newRestfulGenericClient(theServerBase);
			}
		});

		return retVal;
	}
}
