package io.github.wimdeblauwe.errorhandlingspringbootstarter;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.handler.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ErrorHandlingProperties.class)
@ConditionalOnProperty(value = "error.handling.enabled", matchIfMissing = true)
@PropertySource("classpath:/error-handling-defaults.properties")
public class ErrorHandlingConfiguration {

    @Bean
    public ErrorHandlingControllerAdvice errorHandlingControllerAdvice(ErrorHandlingProperties properties,
                                                                       List<ApiExceptionHandler> handlers,
                                                                       FallbackApiExceptionHandler fallbackApiExceptionHandler) {
        return new ErrorHandlingControllerAdvice(properties,
                                                 handlers,
                                                 fallbackApiExceptionHandler);
    }

    @Bean
    public FallbackApiExceptionHandler defaultHandler(ErrorHandlingProperties properties) {
        return new DefaultFallbackApiExceptionHandler(properties);
    }

    @Bean
    public TypeMismatchApiExceptionHandler typeMismatchApiExceptionHandler(ErrorHandlingProperties properties) {
        return new TypeMismatchApiExceptionHandler(properties);
    }

    @Bean
    public ConstraintViolationApiExceptionHandler constraintViolationApiExceptionHandler(ErrorHandlingProperties properties) {
        return new ConstraintViolationApiExceptionHandler(properties);
    }

    @Bean
    public HttpMessageNotReadableApiExceptionHandler httpMessageNotReadableApiExceptionHandler(ErrorHandlingProperties properties) {
        return new HttpMessageNotReadableApiExceptionHandler(properties);
    }

    @Bean
    public MethodArgumentNotValidApiExceptionHandler methodArgumentNotValidApiExceptionHandler(ErrorHandlingProperties properties) {
        return new MethodArgumentNotValidApiExceptionHandler(properties);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.security.access.AccessDeniedException")
    public SpringSecurityApiExceptionHandler springSecurityApiExceptionHandler(ErrorHandlingProperties properties) {
        return new SpringSecurityApiExceptionHandler(properties);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.orm.ObjectOptimisticLockingFailureException")
    public ObjectOptimisticLockingFailureApiExceptionHandler objectOptimisticLockingFailureApiExceptionHandler(ErrorHandlingProperties properties) {
        return new ObjectOptimisticLockingFailureApiExceptionHandler(properties);
    }

    @Bean
    public ApiErrorResponseSerializer apiErrorResponseSerializer(ErrorHandlingProperties properties) {
        return new ApiErrorResponseSerializer(properties);
    }
}
