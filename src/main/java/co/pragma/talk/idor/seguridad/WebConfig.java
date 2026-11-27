package co.pragma.talk.idor.seguridad;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** El "API Gateway" protege las APIs de negocio, igual en la versión vulnerable y en la segura. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiGatewaySimulado gateway;

    public WebConfig(ApiGatewaySimulado gateway) {
        this.gateway = gateway;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(gateway).addPathPatterns("/api/vulnerable/**", "/api/seguro/**");
    }
}
