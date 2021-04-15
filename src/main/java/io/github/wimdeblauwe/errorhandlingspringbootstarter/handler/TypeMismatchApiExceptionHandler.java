package io.github.wimdeblauwe.errorhandlingspringbootstarter.handler;

import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ErrorHandlingProperties;

public class TypeMismatchApiExceptionHandler extends AbstractApiExceptionHandler {
    private MessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();
    private MessageSource messageSource;

    public TypeMismatchApiExceptionHandler(ErrorHandlingProperties properties, MessageSource messageSource) {
        super(properties);
        this.messageSource = messageSource;
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
            return messageSource.getMessage(
                new DefaultMessageSourceResolvable(
                    messageCodesResolver.resolveMessageCodes(errorCode, objectName, fieldName, fieldType),
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
                    escapeSingleQuotes(matme.getMessage())
                ),
                LocaleContextHolder.getLocale()
            );
        } else {
            String errorCode = TypeMismatchException.class.getSimpleName();
            String objectName = exception.getPropertyName();
            Object rejectedValue = exception.getValue();
            return messageSource.getMessage(
                new DefaultMessageSourceResolvable(
                    messageCodesResolver.resolveMessageCodes(errorCode, objectName),
                    new Object[] { 
                        // This allows to "resolve" the object name
                        new DefaultMessageSourceResolvable(
                            new String[] { objectName },
                            objectName
                        ),
                        rejectedValue
                    },
                    escapeSingleQuotes(exception.getMessage())
                ),
                LocaleContextHolder.getLocale()
            );
        }
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
