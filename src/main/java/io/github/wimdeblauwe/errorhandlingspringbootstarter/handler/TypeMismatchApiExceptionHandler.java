package io.github.wimdeblauwe.errorhandlingspringbootstarter.handler;

import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSource;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ErrorHandlingProperties;

public class TypeMismatchApiExceptionHandler extends AbstractApiExceptionHandler {
    public TypeMismatchApiExceptionHandler(ErrorHandlingProperties properties, MessageSource messageSource) {
        super(properties, messageSource);
    }

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof TypeMismatchException;
    }

    @Override
    public ApiErrorResponse handle(Throwable exception) {
        TypeMismatchException ex = (TypeMismatchException) exception;
        ApiErrorResponse response = new ApiErrorResponse(HttpStatus.BAD_REQUEST,
                                                         getErrorCode(exception),
                                                         getMessage(ex));
        response.addErrorProperty("property", getPropertyName(ex));
        response.addErrorProperty("rejectedValue", ex.getValue());
        response.addErrorProperty("expectedType", ex.getRequiredType() != null ? ex.getRequiredType().getName() : null);
        return response;
    }

    private String getPropertyName(TypeMismatchException exception) {
        if (exception instanceof MethodArgumentTypeMismatchException) {
            return ((MethodArgumentTypeMismatchException) exception).getName();
        } else {
            return exception.getPropertyName();
        }
    }

    private String getMessage(TypeMismatchException exception) {
        if (exception instanceof MethodArgumentTypeMismatchException) {
            String errorCode = MethodArgumentTypeMismatchException.class.getSimpleName();
            MethodArgumentTypeMismatchException matme = (MethodArgumentTypeMismatchException) exception;
            String objectName = matme.getName();
            String fieldName = matme.getPropertyName();
            Class<?> fieldType = matme.getParameter().getParameterType();
            Object rejectedValue = matme.getValue();
            return getMessage(
                resolveMessageCodes(errorCode, objectName, fieldName, fieldType),
                new Object[] { 
                    // This allows to "resolve" the object name
                    new DefaultMessageSourceResolvable(
                        new String[] { objectName },
                        objectName),
                    // This allows to "resolve" the field name
                    fieldName==null
                        ?null
                        :new DefaultMessageSourceResolvable(
                            new String[] { fieldName },
                            objectName
                        ),
                    fieldType.getName(),
                    rejectedValue
                },
                matme.getMessage()
            );
        } else {
            String errorCode = TypeMismatchException.class.getSimpleName();
            String objectName = exception.getPropertyName();
            Object rejectedValue = exception.getValue();
            return getMessage(
                resolveMessageCodes(errorCode, objectName),
                new Object[] { 
                    // This allows to "resolve" the object name
                    new DefaultMessageSourceResolvable(
                        new String[] { objectName },
                        objectName
                    ),
                    rejectedValue
                },
                exception.getMessage()
            );
        }
    }
}
