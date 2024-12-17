package ca.uhn.fhir.jpa.starter.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class TomcatSslConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
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

    private final ResourceLoader resourceLoader;

    public TomcatSslConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addConnectorCustomizers(connector -> {
            try {
                // Create temporary files for keystore and truststore
                File tempKeyStore = createTempStoreFile("keystore", keyStorePath);
                File tempTrustStore = createTempStoreFile("truststore", trustStorePath);

                // Get the existing SSL host config
                SSLHostConfig[] sslHostConfigs = connector.getProtocolHandler().findSslHostConfigs();
                if (sslHostConfigs.length > 0) {
                    SSLHostConfig sslHostConfig = sslHostConfigs[0];
                    
                    // Update protocols and ciphers
                    sslHostConfig.setProtocols("TLSv1.2");
                    sslHostConfig.setCiphers("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");

                    // Update certificate configuration
                    SSLHostConfigCertificate cert = sslHostConfig.getCertificates().iterator().next();
                    cert.setCertificateKeystoreFile(tempKeyStore.getAbsolutePath());
                    cert.setCertificateKeystorePassword(keyStorePassword);
                    cert.setCertificateKeystoreType(keyStoreType);

                    // Update truststore configuration
                    sslHostConfig.setTruststoreFile(tempTrustStore.getAbsolutePath());
                    sslHostConfig.setTruststorePassword(trustStorePassword);
                    sslHostConfig.setTruststoreType(trustStoreType);

                    logger.info("Successfully updated SSL configuration with keystore and truststore from classpath");

                    // Register shutdown hook to delete temporary files
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            Files.deleteIfExists(tempKeyStore.toPath());
                            Files.deleteIfExists(tempTrustStore.toPath());
                        } catch (IOException e) {
                            logger.warn("Failed to delete temporary store files", e);
                        }
                    }));
                }
            } catch (IOException e) {
                logger.error("Failed to configure SSL connector", e);
                throw new RuntimeException("Failed to configure SSL", e);
            }
        });
    }

    private File createTempStoreFile(String prefix, String resourcePath) throws IOException {
        Resource resource = resourceLoader.getResource(resourcePath);
        File tempFile = File.createTempFile(prefix, ".tmp");
        tempFile.deleteOnExit();

        try (InputStream is = resource.getInputStream();
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            is.transferTo(fos);
        }
        return tempFile;
    }

    @Bean
    public SSLContext sslContext() throws Exception {
        try {
            // Load keystore from classpath
            Resource keyStoreResource = resourceLoader.getResource(keyStorePath);
            if (!keyStoreResource.exists()) {
                throw new IOException("Keystore file not found: " + keyStorePath);
            }
            KeyStore keystore = loadKeyStore(keyStoreResource, keyStorePassword, keyStoreType);
            logger.info("Successfully loaded keystore from: {}", keyStorePath);

            // Load truststore from classpath
            Resource trustStoreResource = resourceLoader.getResource(trustStorePath);
            if (!trustStoreResource.exists()) {
                throw new IOException("Truststore file not found: " + trustStorePath);
            }
            KeyStore truststore = loadKeyStore(trustStoreResource, trustStorePassword, trustStoreType);
            logger.info("Successfully loaded truststore from: {}", trustStorePath);

            // Initialize key manager
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, keyStorePassword.toCharArray());
            logger.info("Successfully initialized key manager");

            // Initialize trust manager
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(truststore);
            logger.info("Successfully initialized trust manager");

            // Create SSL context with TLS
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            logger.info("Successfully initialized SSL context");
            
            return sslContext;
        } catch (Exception e) {
            logger.error("Failed to initialize SSL context", e);
            throw e;
        }
    }

    private KeyStore loadKeyStore(Resource resource, String password, String type) 
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore store = KeyStore.getInstance(type);
        try (InputStream is = resource.getInputStream()) {
            store.load(is, password.toCharArray());
            return store;
        }
    }
} 