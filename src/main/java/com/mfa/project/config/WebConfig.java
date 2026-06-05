package com.mfa.project.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final HardwareAuthInterceptor hardwareAuthInterceptor;
    public WebConfig(HardwareAuthInterceptor hardwareAuthInterceptor) {
        this.hardwareAuthInterceptor = hardwareAuthInterceptor;
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
    }
}
