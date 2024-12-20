package ca.uhn.fhir.jpa.starter.common;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;

public class BearerTokenAuthInterceptor implements IClientInterceptor {
    private final String token;

    public BearerTokenAuthInterceptor(String token) {
        this.token = token;
    }

    @Override
    public void interceptRequest(IHttpRequest theRequest) {
        theRequest.addHeader("Authorization", "Bearer " + token);
    }

    @Override
    public void interceptResponse(IHttpResponse theResponse) {
        // Nothing to do here
    }
} 