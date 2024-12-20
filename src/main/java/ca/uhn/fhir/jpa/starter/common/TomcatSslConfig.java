package ca.uhn.fhir.jpa.starter.common;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class TomcatSslConfig {
    private static final Logger logger = LoggerFactory.getLogger(TomcatSslConfig.class);

    @Value("${server.ssl.key-store}")
    private String keyStorePath;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${server.ssl.key-store-type}")
    private String keyStoreType;

    @Value("${server.ssl.trust-store}")
    private String trustStorePath;

    @Value("${server.ssl.trust-store-password}")
    private String trustStorePassword;

    @Value("${server.ssl.trust-store-type}")
    private String trustStoreType;

    @Value("${server.ssl.client-auth:want}")
    private String clientAuth;

    private final ResourceLoader resourceLoader;

    public TomcatSslConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public SSLContext sslContext() throws Exception {
        try {
            // Load keystore
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            Resource keyStoreResource = resourceLoader.getResource(keyStorePath);
            try (InputStream inputStream = keyStoreResource.getInputStream()) {
                keyStore.load(inputStream, keyStorePassword.toCharArray());
            }
            logger.info("Successfully loaded keystore from: {}", keyStorePath);

            // Initialize key manager
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
            logger.info("Successfully initialized key manager");

            // Load truststore
            KeyStore trustStore = KeyStore.getInstance(trustStoreType);
            Resource trustStoreResource = resourceLoader.getResource(trustStorePath);
            try (InputStream inputStream = trustStoreResource.getInputStream()) {
                trustStore.load(inputStream, trustStorePassword.toCharArray());
            }
            logger.info("Successfully loaded truststore from: {}", trustStorePath);

            // Initialize trust manager
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            logger.info("Successfully initialized trust manager");

            // Create SSL context
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers(),
                new SecureRandom()
            );
            logger.info("Successfully initialized SSL context with client auth: {}", clientAuth);
            
            return sslContext;
        } catch (Exception e) {
            logger.error("Failed to initialize SSL context", e);
            throw e;
        }
    }
}