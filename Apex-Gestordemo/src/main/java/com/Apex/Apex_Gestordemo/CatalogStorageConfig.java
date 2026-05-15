package com.Apex.Apex_Gestordemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class CatalogStorageConfig implements WebMvcConfigurer {

    @Value("${apex.catalog.enrichment.storage-dir:${user.dir}/storage/catalog}")
    private String storageDir;

    @Value("${apex.catalog.enrichment.public-path:/media/catalog}")
    private String publicPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String pattern = publicPath.replaceAll("/$", "") + "/**";
        String location = Path.of(storageDir).toUri().toString();
        registry.addResourceHandler(pattern).addResourceLocations(location);
    }
}
