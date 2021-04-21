package io.github.wimdeblauwe.errorhandlingspringbootstarter.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.IgnoredPropertyException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.PropertyBindingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.MessageCodesResolver;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ErrorHandlingProperties;

/**
 * {@link io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiExceptionHandler} for
 * {@link HttpMessageNotReadableException}. This typically happens when Spring can't properly
 * decode the incoming request to JSON.
 */
public class HttpMessageNotReadableApiExceptionHandler extends AbstractApiExceptionHandler {
    private MessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();
    private MessageSource messageSource;

    public HttpMessageNotReadableApiExceptionHandler(ErrorHandlingProperties properties, MessageSource messageSource) {
        super(properties);
        this.messageSource = messageSource;
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
            return getMessage((JsonProcessingException) cause);
        }
        String errorCode = HttpMessageNotReadableException.class.getSimpleName();
        return messageSource.getMessage(
            new DefaultMessageSourceResolvable(
                new String[] { errorCode },
                exception.getMessage()
            ),
            LocaleContextHolder.getLocale()
        );
    }

    private String getMessage(UnrecognizedPropertyException exception) {
        String baseKey = UnrecognizedPropertyException.class.getSimpleName();
        String objectName = getObjectNameFromPath(exception.getPathReference());
        return messageSource.getMessage(
            new DefaultMessageSourceResolvable(
                messageCodesResolver.resolveMessageCodes(baseKey, objectName),
                new Object[] { exception.getPropertyName() },
                escapeSingleQuotes(exception.getMessage())
            ),
            LocaleContextHolder.getLocale()
        );
    }

    private String getMessage(IgnoredPropertyException exception) {
        String baseKey = IgnoredPropertyException.class.getSimpleName();
        String objectName = getObjectNameFromPath(exception.getPathReference());
        return messageSource.getMessage(
            new DefaultMessageSourceResolvable(
                messageCodesResolver.resolveMessageCodes(baseKey, objectName),
                new Object[] { exception.getPropertyName() },
                escapeSingleQuotes(exception.getMessage())
            ),
            LocaleContextHolder.getLocale()
        );
    }

    private String getMessage(InvalidFormatException exception) {
        String baseKey = InvalidFormatException.class.getSimpleName();
        String objectName = getObjectNameFromPath(exception.getPathReference());
        String field = exception.getPath().get(0).getFieldName();
        Object value = exception.getValue();
        return messageSource.getMessage(
            new DefaultMessageSourceResolvable(
                messageCodesResolver.resolveMessageCodes(baseKey, objectName, field, null),
                new Object[] { value },
                escapeSingleQuotes(exception.getMessage())
            ),
            LocaleContextHolder.getLocale()
        );
    }

    private String getMessage(PropertyBindingException exception) {
        String baseKey = PropertyBindingException.class.getSimpleName();
        String objectName = getObjectNameFromPath(exception.getPathReference());
        return messageSource.getMessage(
            new DefaultMessageSourceResolvable(
                messageCodesResolver.resolveMessageCodes(baseKey, objectName),
                new Object[] { exception.getPropertyName() },
                escapeSingleQuotes(exception.getMessage())
            ),
            LocaleContextHolder.getLocale()
        );
    }

    private String getMessage(JsonProcessingException exception) {
        String errorCode = JsonProcessingException.class.getSimpleName();
        return messageSource.getMessage(
            new DefaultMessageSourceResolvable(
                new String[] { errorCode },
                exception.getMessage()
            ),
            LocaleContextHolder.getLocale()
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
