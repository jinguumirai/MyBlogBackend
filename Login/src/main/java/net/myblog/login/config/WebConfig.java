package net.myblog.login.config;

import jakarta.annotation.Nonnull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configurePathMatch(@Nonnull PathMatchConfigurer pathMatchConfigurer)
    {
        // pathMatchConfigurer.addPathPrefix("/identify", c -> c.isAnnotationPresent(RestController.class));
    }
}
