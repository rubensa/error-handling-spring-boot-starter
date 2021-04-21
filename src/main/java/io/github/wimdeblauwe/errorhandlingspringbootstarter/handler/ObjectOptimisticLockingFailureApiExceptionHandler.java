package io.github.wimdeblauwe.errorhandlingspringbootstarter.handler;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.MessageCodesResolver;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ErrorHandlingProperties;

public class ObjectOptimisticLockingFailureApiExceptionHandler extends AbstractApiExceptionHandler {
    private MessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();
    private MessageSource messageSource;

    public ObjectOptimisticLockingFailureApiExceptionHandler(ErrorHandlingProperties properties, MessageSource messageSource) {
        super(properties);
        this.messageSource = messageSource;
    }

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof ObjectOptimisticLockingFailureException;
    }

    @Override
    public ApiErrorResponse handle(Throwable exception) {
        ObjectOptimisticLockingFailureException ex = (ObjectOptimisticLockingFailureException) exception;
        ApiErrorResponse response = new ApiErrorResponse(HttpStatus.CONFLICT,
                                                         getErrorCode(exception),
                                                         getMessage(ex));
        response.addErrorProperty("identifier", ex.getIdentifier());
        response.addErrorProperty("persistentClassName", ex.getPersistentClassName());
        return response;
    }

    private String getMessage(ObjectOptimisticLockingFailureException exception) {
        String errorCode = ObjectOptimisticLockingFailureException.class.getSimpleName();
        String persistentClassName = exception.getPersistentClassName();
        Object identifier = exception.getIdentifier();
        return messageSource.getMessage(
            new DefaultMessageSourceResolvable(
                messageCodesResolver.resolveMessageCodes(errorCode, persistentClassName),
                new Object[] { 
                    // This allows to "resolve" the object name
                    new DefaultMessageSourceResolvable(
                        new String[] { persistentClassName },
                        persistentClassName
                    ),
                    identifier
                },
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
