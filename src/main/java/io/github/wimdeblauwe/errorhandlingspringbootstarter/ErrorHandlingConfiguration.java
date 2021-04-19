package io.github.wimdeblauwe.errorhandlingspringbootstarter;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.handler.ConstraintViolationApiExceptionHandler;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.handler.HttpMessageNotReadableApiExceptionHandler;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.handler.MethodArgumentNotValidApiExceptionHandler;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.handler.ObjectOptimisticLockingFailureApiExceptionHandler;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.handler.SpringSecurityApiExceptionHandler;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.handler.TypeMismatchApiExceptionHandler;

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
    public TypeMismatchApiExceptionHandler typeMismatchApiExceptionHandler(ErrorHandlingProperties properties, MessageSource messageSource) {
        return new TypeMismatchApiExceptionHandler(properties, messageSource);
    }

    @Bean
    public ConstraintViolationApiExceptionHandler constraintViolationApiExceptionHandler(ErrorHandlingProperties properties, MessageSource messageSource) {
        return new ConstraintViolationApiExceptionHandler(properties, messageSource);
    }

    @Bean
    public HttpMessageNotReadableApiExceptionHandler httpMessageNotReadableApiExceptionHandler(ErrorHandlingProperties properties) {
        return new HttpMessageNotReadableApiExceptionHandler(properties);
    }

    @Bean
    public MethodArgumentNotValidApiExceptionHandler methodArgumentNotValidApiExceptionHandler(ErrorHandlingProperties properties, MessageSource messageSource) {
        return new MethodArgumentNotValidApiExceptionHandler(properties, messageSource);
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
