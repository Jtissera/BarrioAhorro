package com.barrioahorro.api.config

import com.barrioahorro.api.modules.business.PhotoStorage
import com.barrioahorro.api.modules.business.UPLOADS_URL_PREFIX
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class UploadsWebConfig(
    private val photoStorage: PhotoStorage,
) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("$UPLOADS_URL_PREFIX**")
            .addResourceLocations(photoStorage.directory.toUri().toString())
    }
}
