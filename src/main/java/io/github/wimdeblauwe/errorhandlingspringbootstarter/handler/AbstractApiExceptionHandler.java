package io.github.wimdeblauwe.errorhandlingspringbootstarter.handler;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.lang.Nullable;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.MessageCodesResolver;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiExceptionHandler;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ErrorHandlingProperties;

public abstract class AbstractApiExceptionHandler implements ApiExceptionHandler{
    protected final ErrorHandlingProperties properties;
    private MessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();
    private MessageSource messageSource;

    public AbstractApiExceptionHandler(ErrorHandlingProperties properties) {
        this(properties, null);
    }

    public AbstractApiExceptionHandler(ErrorHandlingProperties properties, @Nullable MessageSource messageSource) {
        this.properties = properties;
        this.messageSource = messageSource;
    }

    protected String getErrorCode(Throwable exception) {
        return replaceCodeWithConfiguredOverrideIfPresent(exception.getClass().getName());
    }

    protected String replaceCodeWithConfiguredOverrideIfPresent(String code) {
        return properties.getCodes().getOrDefault(code, code);
    }

    protected boolean hasConfiguredOverrideForCode(String code) {
        return properties.getCodes().containsKey(code);
    }

    protected boolean hasConfiguredOverrideForMessage(String key) {
        return properties.getMessages().containsKey(key);
    }

    protected String getOverrideMessage(String key) {
        return properties.getMessages().get(key);
    }

    String getMessage(String code) {
        return getMessage(new String[] {code}, null, null);
    }

    String getMessage(String code, String defaultMessage) {
        return getMessage(new String[] {code}, null, defaultMessage);
    }

    String getMessage(String code, Object[] arguments, String defaultMessage) {
        return getMessage(new String[] {code}, arguments, defaultMessage);
    }

    String getMessage(String[] codes) {
        return getMessage(codes, null, null);
    }

    String getMessage(String[] codes, String defaultMessage) {
        return getMessage(codes, null, defaultMessage);
    }

    String getMessage(@Nullable String[] codes, @Nullable Object[] arguments, @Nullable String defaultMessage) {
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

    String getMessage(MessageSourceResolvable resolvable) {
        if (messageSource == null) {
            return resolvable.getDefaultMessage();
        }
        return messageSource.getMessage(resolvable, LocaleContextHolder.getLocale());
    }

    String getMessage(Throwable exception, Class<?> exceptionClass) {
        String errorCode = exceptionClass.getSimpleName();
        return getMessage(
            errorCode,
            exception.getMessage()
        );
    }

    String[] resolveMessageCodes(String errorCode, String objectName) {
        return messageCodesResolver.resolveMessageCodes(errorCode, objectName);
    }

    String[] resolveMessageCodes(String errorCode, String objectName, String field, @Nullable Class<?> fieldType) {
        return messageCodesResolver.resolveMessageCodes(errorCode, objectName, field, fieldType);
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
