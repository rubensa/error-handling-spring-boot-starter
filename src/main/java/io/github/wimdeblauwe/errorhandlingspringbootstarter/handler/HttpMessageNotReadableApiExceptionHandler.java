package io.github.wimdeblauwe.errorhandlingspringbootstarter.handler;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ErrorHandlingProperties;

/**
 * {@link io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiExceptionHandler} for
 * {@link HttpMessageNotReadableException}. This typically happens when Spring can't properly
 * decode the incoming request to JSON.
 */
public class HttpMessageNotReadableApiExceptionHandler extends AbstractApiExceptionHandler {
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
        String errorCode = HttpMessageNotReadableException.class.getSimpleName();
        return messageSource.getMessage(
            new DefaultMessageSourceResolvable(
                new String[] {errorCode},
                new Object[] { exception.getHttpInputMessage() },
                escapeSingleQuotes(exception.getMessage())
            ),
            LocaleContextHolder.getLocale()
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
