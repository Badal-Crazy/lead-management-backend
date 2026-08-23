package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;
    
    @Value("${server.port:8080}")
    private int serverPort;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getApiBaseUrl() {
        return baseUrl + "/api";
    }
}
