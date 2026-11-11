package com.uniquindio.teen_sport.integration;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP de Brevo con tiempos de espera acotados, para que un proveedor lento no retenga los
 * hilos de envío indefinidamente.
 */
@Configuration
public class BrevoConfiguration {

    /**
     * @param properties configuración de Brevo
     * @return cliente apuntado a la API v3 de Brevo
     */
    @Bean
    public RestClient brevoRestClient(BrevoProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
