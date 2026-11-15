package net.myBlog.common.configs;

import net.myBlog.common.handlers.GlobalExceptionHandler;
import net.myBlog.common.properties.ExceptionProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(ExceptionProperties.class)
@ConditionalOnProperty(
        prefix = "common.exception",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true   // 关键：配置文件中没写这个属性时，默认视为 true
)
public class ExceptionAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler(ExceptionProperties exceptionProperties, MessageSource messageSource) {
        return new GlobalExceptionHandler(exceptionProperties, messageSource);
    }
}
