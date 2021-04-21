package io.github.wimdeblauwe.errorhandlingspringbootstarter.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.IgnoredPropertyException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.PropertyBindingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ErrorHandlingProperties;

/**
 * {@link io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiExceptionHandler} for
 * {@link HttpMessageNotReadableException}. This typically happens when Spring can't properly
 * decode the incoming request to JSON.
 */
public class HttpMessageNotReadableApiExceptionHandler extends AbstractApiExceptionHandler {
    public HttpMessageNotReadableApiExceptionHandler(ErrorHandlingProperties properties, MessageSource messageSource) {
        super(properties, messageSource);
    }

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof HttpMessageNotReadableException;
    }

    @Override
    public ApiErrorResponse handle(Throwable exception) {
        return new ApiErrorResponse(HttpStatus.BAD_REQUEST,
                                    replaceCodeWithConfiguredOverrideIfPresent(exception.getClass().getName()),
                                    getMessage((HttpMessageNotReadableException)exception));
    }

    private String getMessage(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof UnrecognizedPropertyException) {
            return getMessage((UnrecognizedPropertyException) cause);
        }
        if (cause instanceof IgnoredPropertyException) {
            return getMessage((IgnoredPropertyException) cause);
        }
        if (cause instanceof InvalidFormatException) {
            return getMessage((InvalidFormatException) cause);
        }
        if (cause instanceof PropertyBindingException) {
            return getMessage((PropertyBindingException) cause);
        }
        if (cause instanceof JsonProcessingException) {
            return getMessage(cause, JsonProcessingException.class);
        }
        return getMessage(cause, HttpMessageNotReadableException.class);
    }

    private String getMessage(UnrecognizedPropertyException exception) {
        String baseKey = UnrecognizedPropertyException.class.getSimpleName();
        String objectName = getObjectNameFromPath(exception.getPathReference());
        return getMessage(
            resolveMessageCodes(baseKey, objectName),
            new Object[] { exception.getPropertyName() },
            exception.getMessage()
        );
    }

    private String getMessage(IgnoredPropertyException exception) {
        String baseKey = IgnoredPropertyException.class.getSimpleName();
        String objectName = getObjectNameFromPath(exception.getPathReference());
        return getMessage(
            resolveMessageCodes(baseKey, objectName),
            new Object[] { exception.getPropertyName() },
            exception.getMessage()
        );
    }

    private String getMessage(InvalidFormatException exception) {
        String baseKey = InvalidFormatException.class.getSimpleName();
        String objectName = getObjectNameFromPath(exception.getPathReference());
        String field = exception.getPath().get(0).getFieldName();
        Object value = exception.getValue();
        return getMessage(
            resolveMessageCodes(baseKey, objectName, field, null),
            new Object[] { value },
            exception.getMessage()
        );
    }

    private String getMessage(PropertyBindingException exception) {
        String baseKey = PropertyBindingException.class.getSimpleName();
        String objectName = getObjectNameFromPath(exception.getPathReference());
        return getMessage(
            resolveMessageCodes(baseKey, objectName),
            new Object[] { exception.getPropertyName() },
            exception.getMessage()
        );
    }

    /**
     * Gets the object name from path.
     *
     * @param path
     *            the path
     * @return the object name from path
     */
    protected String getObjectNameFromPath(String path) {
        // Example: [com.kirogrifols.kirolink.kirofill.api.User["id"]]
        int startPos = path.lastIndexOf('.');
        int endPos = path.lastIndexOf('[');
        String name = path.substring(startPos + 1, endPos);
        return StringUtils.uncapitalize(name);
    }
}
