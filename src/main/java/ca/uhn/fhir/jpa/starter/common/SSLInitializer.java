package ca.uhn.fhir.jpa.starter.common;

import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SSLInitializer implements BeanFactoryPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SSLInitializer.class);
    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        try {
            logger.info("Initializing global SSL trust settings");
            
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        logger.info("getAcceptedIssuers called - SSL verification happening");
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        logger.info("checkClientTrusted called with authType: {} - SSL verification happening", authType);
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        logger.info("checkServerTrusted called with authType: {} - SSL verification happening", authType);
                    }
                }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            
            // Set as default SSL context
            SSLContext.setDefault(sc);
            
            // Also set for HttpsURLConnection
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            
            // Set system properties as well
            System.setProperty("javax.net.ssl.trustStore", "NONE");
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");
            System.setProperty("javax.net.ssl.trustStorePassword", "");
            System.setProperty("javax.net.ssl.trustAll", "true");
            
            logger.info("Global SSL trust settings initialized");
        } catch (Exception e) {
            logger.error("Failed to initialize SSL settings", e);
            throw new RuntimeException("Failed to initialize SSL settings", e);
        }
    }
} 