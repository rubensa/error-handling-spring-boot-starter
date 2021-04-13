package io.github.wimdeblauwe.errorhandlingspringbootstarter.handler;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.FieldError;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiFieldError;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiGlobalError;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ErrorHandlingProperties;

/**
 * Class to handle {@link MethodArgumentNotValidException} exceptions. This is typically
 * used when `@Valid` is used on {@link org.springframework.web.bind.annotation.RestController}
 * method arguments.
 */
public class MethodArgumentNotValidApiExceptionHandler extends AbstractApiExceptionHandler {
    private MessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();
    private MessageSource messageSource;

    public MethodArgumentNotValidApiExceptionHandler(ErrorHandlingProperties properties, MessageSource messageSource) {
        super(properties);
        this.messageSource = messageSource;
    }

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof MethodArgumentNotValidException;
    }

    @Override
    public ApiErrorResponse handle(Throwable exception) {

        MethodArgumentNotValidException ex = (MethodArgumentNotValidException) exception;
        ApiErrorResponse response = new ApiErrorResponse(HttpStatus.BAD_REQUEST,
                                                         getErrorCode(exception),
                                                         getMessage(ex));
        BindingResult bindingResult = ex.getBindingResult();
        if (bindingResult.hasFieldErrors()) {
            bindingResult.getFieldErrors().stream()
                         .map(fieldError -> new ApiFieldError(getCode(fieldError),
                                                              fieldError.getField(),
                                                              getMessage(fieldError),
                                                              fieldError.getRejectedValue()))
                         .forEach(response::addFieldError);
        }

        if (bindingResult.hasGlobalErrors()) {
            bindingResult.getGlobalErrors().stream()
                         .map(globalError -> new ApiGlobalError(replaceCodeWithConfiguredOverrideIfPresent(globalError.getCode()),
                                                                getMessage(globalError)))
                         .forEach(response::addGlobalError);
        }

        return response;
    }

    private String getCode(FieldError fieldError) {
        String fieldSpecificCode = fieldError.getField() + "." + fieldError.getCode();
        if (hasConfiguredOverrideForCode(fieldSpecificCode)) {
            return replaceCodeWithConfiguredOverrideIfPresent(fieldSpecificCode);
        }
        return replaceCodeWithConfiguredOverrideIfPresent(fieldError.getCode());
    }

    private String getMessage(FieldError fieldError) {
        String fieldSpecificKey = fieldError.getField() + "." + fieldError.getCode();
        if (hasConfiguredOverrideForMessage(fieldSpecificKey)) {
            return getOverrideMessage(fieldSpecificKey);
        }
        if (hasConfiguredOverrideForMessage(fieldError.getCode())) {
            return getOverrideMessage(fieldError.getCode());
        }
        return messageSource.getMessage(fieldError, LocaleContextHolder.getLocale());
    }

    private String getMessage(ObjectError objectError) {
        if (hasConfiguredOverrideForMessage(objectError.getCode())) {
            return getOverrideMessage(objectError.getCode());
        }
        return messageSource.getMessage(objectError, LocaleContextHolder.getLocale());
    }

    private String getMessage(MethodArgumentNotValidException exception) {
        String errorCode = MethodArgumentNotValidException.class.getSimpleName();
        String objectName = exception.getBindingResult().getObjectName();
        return messageSource.getMessage(
            new DefaultMessageSourceResolvable(
                messageCodesResolver.resolveMessageCodes(errorCode, objectName),
                new Object[] { 
                    // This allows to "resolve" the object name
                    new DefaultMessageSourceResolvable(
                        new String[] { exception.getBindingResult().getObjectName() },
                        exception.getBindingResult().getObjectName()
                    ),
                    exception.getBindingResult().getErrorCount()
                },
                escapeSingleQuotes("Validation failed for object='" + exception.getBindingResult().getObjectName() + "'. Error count: " + exception.getBindingResult().getErrorCount())
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
