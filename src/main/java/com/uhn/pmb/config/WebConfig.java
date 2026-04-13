package com.uhn.pmb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;
import java.io.File;

/**
 * Web Configuration for serving static resources including uploaded files
 * Enables GET requests to /uploads/** to access files from uploads/ directory
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ensure uploads directory exists
        File uploadsFolder = new File(uploadDir);
        if (!uploadsFolder.exists()) {
            uploadsFolder.mkdirs();
        }

        // Register resource handler for /uploads/** to serve files from uploads/ directory
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + new File(uploadDir).getAbsolutePath() + "/")
                .setCachePeriod(3600); // Cache for 1 hour

        // Keep default static resource locations
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }
}
