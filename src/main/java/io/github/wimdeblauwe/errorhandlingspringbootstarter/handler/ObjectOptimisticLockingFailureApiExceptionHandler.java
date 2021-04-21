package io.github.wimdeblauwe.errorhandlingspringbootstarter.handler;

import org.springframework.context.MessageSource;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ErrorHandlingProperties;

public class ObjectOptimisticLockingFailureApiExceptionHandler extends AbstractApiExceptionHandler {
    public ObjectOptimisticLockingFailureApiExceptionHandler(ErrorHandlingProperties properties, MessageSource messageSource) {
        super(properties, messageSource);
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
        return getMessage(
            resolveMessageCodes(errorCode, persistentClassName),
            new Object[] { 
                // This allows to "resolve" the object name
                new DefaultMessageSourceResolvable(
                    new String[] { persistentClassName },
                    persistentClassName
                ),
                identifier
            },
            exception.getMessage()
        );
    }
}
