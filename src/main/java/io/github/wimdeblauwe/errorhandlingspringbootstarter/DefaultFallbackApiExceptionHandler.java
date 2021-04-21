package io.github.wimdeblauwe.errorhandlingspringbootstarter;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;


public class DefaultFallbackApiExceptionHandler implements FallbackApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFallbackApiExceptionHandler.class);

    private final ErrorHandlingProperties properties;
    private MessageSource messageSource;

    public DefaultFallbackApiExceptionHandler(ErrorHandlingProperties properties) {
        this(properties, null);
    }

    public DefaultFallbackApiExceptionHandler(ErrorHandlingProperties properties, @Nullable MessageSource messageSource) {
        this.properties = properties;
        this.messageSource = messageSource;
    }

    @Override
    public ApiErrorResponse handle(Throwable exception) {
        HttpStatus statusCode = getHttpStatus(exception);
        String errorCode = getErrorCode(exception);

        // Using a TreeMap for alphabetical ordering of property names
        Map<String, Object> errorProperties = new TreeMap<>();
        errorProperties.putAll(getMethodResponseErrorProperties(exception));
        errorProperties.putAll(getFieldResponseErrorProperties(exception));
        ApiErrorResponse response = new ApiErrorResponse(statusCode, errorCode, getMessage(exception, exception.getClass(), errorProperties));
        response.addErrorProperties(errorProperties);

        return response;
    }

    private Map<String, Object> getFieldResponseErrorProperties(Throwable exception) {
        Map<String, Object> result = new HashMap<>();
        for (Field field : exception.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(ResponseErrorProperty.class)) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(exception);
                    if (value != null || field.getAnnotation(ResponseErrorProperty.class).includeIfNull()) {
                        result.put(getPropertyName(field), value);
                    }
                } catch (IllegalAccessException e) {
                    LOGGER.error(String.format("Unable to use field result of field %s.%s", exception.getClass().getName(), field.getName()));
                }
            }
        }
        return result;
    }

    private Map<String, Object> getMethodResponseErrorProperties(Throwable exception) {
        // Using a TreeMap for alphabetical ordering of property names
        Map<String, Object> result = new TreeMap<>();
        Class<? extends Throwable> exceptionClass = exception.getClass();
        for (Method method : exceptionClass.getMethods()) {
            if (method.isAnnotationPresent(ResponseErrorProperty.class)
                    && method.getReturnType() != Void.TYPE
                    && method.getParameterCount() == 0) {
                try {
                    method.setAccessible(true);

                    Object value = method.invoke(exception);
                    if (value != null || method.getAnnotation(ResponseErrorProperty.class).includeIfNull()) {
                        result.put(getPropertyName(exceptionClass, method),
                                   value);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LOGGER.error(String.format("Unable to use method result of method %s.%s", exceptionClass.getName(), method.getName()));
                }
            }
        }
        return result;
    }

    private String getPropertyName(Field field) {
        ResponseErrorProperty annotation = AnnotationUtils.getAnnotation(field, ResponseErrorProperty.class);
        assert annotation != null;
        if (!StringUtils.isEmpty(annotation.value())) {
            return annotation.value();
        }

        return field.getName();
    }

    private String getPropertyName(Class<? extends Throwable> exceptionClass, Method method) {
        ResponseErrorProperty annotation = AnnotationUtils.getAnnotation(method, ResponseErrorProperty.class);
        assert annotation != null;
        if (!StringUtils.isEmpty(annotation.value())) {
            return annotation.value();
        }

        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(exceptionClass);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                if (propertyDescriptor.getReadMethod().equals(method)) {
                    return propertyDescriptor.getName();
                }
            }
        } catch (IntrospectionException e) {
            //ignore
        }

        return method.getName();
    }

    private HttpStatus getHttpStatus(Throwable exception) {
        ResponseStatus responseStatus = AnnotationUtils.getAnnotation(exception.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            return responseStatus.value();
        }

        if (exception instanceof ResponseStatusException) {
            return ((ResponseStatusException) exception).getStatus();
        }

        return properties.getHttpStatuses().getOrDefault(exception.getClass().getName(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getErrorCode(Throwable exception) {
        ResponseErrorCode errorCodeAnnotation = AnnotationUtils.getAnnotation(exception.getClass(), ResponseErrorCode.class);
        String code;
        if (errorCodeAnnotation != null) {
            code = errorCodeAnnotation.value();
        } else {
            String exceptionClassName = exception.getClass().getName();
            if (properties.getCodes().containsKey(exceptionClassName)) {
                code = replaceCodeWithConfiguredOverrideIfPresent(exceptionClassName);
            } else {
                switch (properties.getDefaultErrorCodeStrategy()) {
                    case FULL_QUALIFIED_NAME:
                        code = exception.getClass().getName();
                        break;
                    case ALL_CAPS:
                        code = convertToAllCaps(exception.getClass().getSimpleName());
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown default error code strategy: " + properties.getDefaultErrorCodeStrategy());
                }
            }
        }

        return code;
    }

    private String convertToAllCaps(String exceptionClassName) {
        String result = exceptionClassName.replaceFirst("Exception$", "");
        result = result.replaceAll("([a-z])([A-Z]+)", "$1_$2").toUpperCase(Locale.ENGLISH);
        return result;
    }

    private String replaceCodeWithConfiguredOverrideIfPresent(String code) {
        return properties.getCodes().getOrDefault(code, code);
    }

    private String getMessage(String code, Object[] arguments, String defaultMessage) {
        return getMessage(new String[] {code}, arguments, defaultMessage);
    }

    private String getMessage(@Nullable String[] codes, @Nullable Object[] arguments, @Nullable String defaultMessage) {
        if (messageSource == null) {
            return defaultMessage;
        }
        return messageSource.getMessage(
            new DefaultMessageSourceResolvable(
                codes,
                arguments,
                (arguments == null)?defaultMessage:escapeSingleQuotes(defaultMessage)
            ),
            LocaleContextHolder.getLocale()
        );
    }

    private String getMessage(Throwable exception, Class<?> exceptionClass, Map<String, Object> propertyMap) {
        List<Object> arguments = new ArrayList<>();
        for(Map.Entry<String,Object> property : propertyMap.entrySet()) {
            String propertyName = property.getKey();
            Object propertyValue = property.getValue();
            // This allows to "resolve" the propertyName
            arguments.add(
                new DefaultMessageSourceResolvable(
                    new String[] { propertyName },
                    propertyName
                )
            );
            arguments.add(propertyValue);
        }
        String errorCode = exceptionClass.getSimpleName();
        return getMessage(
            errorCode,
            arguments.toArray(),
            exception.getMessage()
        );
    }

    /**
     * When the message has parameters, a MessageFormat is applied.
     * Whenever you are using MessageFormat you should be aware that 
     * the single quote character (') fulfils a special purpose inside 
     * message patterns. The single quote is used to represent a section 
     * within the message pattern that will not be formatted. A single 
     * quote itself must be escaped by using two single quotes ('').
     */
    private String escapeSingleQuotes(String message) {
        return message.replace("'", "''");
    }
}
