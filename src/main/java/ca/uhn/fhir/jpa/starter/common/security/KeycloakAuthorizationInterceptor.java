package ca.uhn.fhir.jpa.starter.common.security;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KeycloakAuthorizationInterceptor extends AuthorizationInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakAuthorizationInterceptor.class);
    private static final String REQUIRED_ROLE = "fhir-api";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final JwtDecoder jwtDecoder;

    public KeycloakAuthorizationInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
        String authHeader = theRequestDetails.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            logger.warn("Missing or invalid Authorization header");
            throw new AuthenticationException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        
        try {
            Jwt jwt = jwtDecoder.decode(token);
            if (!hasRequiredRole(jwt)) {
                logger.warn("Token missing required role: {}", REQUIRED_ROLE);
                throw new AuthenticationException("Insufficient permissions");
            }

            // If we get here, allow access to everything
            return new RuleBuilder()
                .allowAll()
                .build();
                
        } catch (Exception e) {
            logger.error("Error validating token", e);
            throw new AuthenticationException("Invalid token");
        }
    }

    private boolean hasRequiredRole(Jwt jwt) {
        try {
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess == null) return false;

            @SuppressWarnings("unchecked")
            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("hapi-fhir");
            if (clientAccess == null) return false;

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) clientAccess.get("roles");
            return roles != null && roles.contains(REQUIRED_ROLE);
            
        } catch (Exception e) {
            logger.error("Error checking roles in token", e);
            return false;
        }
    }
} 
