package io.github.wimdeblauwe.errorhandlingspringbootstarter.handler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiFieldError;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiGlobalError;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ErrorHandlingProperties;

/**
 * {@link io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiExceptionHandler} for
 * {@link ConstraintViolationException}. This typically happens when there is validation
 * on Spring services that gets triggered.
 *
 * @see MethodArgumentNotValidApiExceptionHandler
 */
public class ConstraintViolationApiExceptionHandler extends AbstractApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintViolationApiExceptionHandler.class);
    /** The Constant internalAnnotationAttributes. */
    private static final Set<String> internalAnnotationAttributes = new HashSet<>(
            3);

    static {
        internalAnnotationAttributes.add("message");
        internalAnnotationAttributes.add("groups");
        internalAnnotationAttributes.add("payload");
    }

    public ConstraintViolationApiExceptionHandler(ErrorHandlingProperties properties, MessageSource messageSource) {
        super(properties, messageSource);
    }

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof ConstraintViolationException;
    }

    @Override
    public ApiErrorResponse handle(Throwable exception) {

        ConstraintViolationException ex = (ConstraintViolationException) exception;
        ApiErrorResponse response = new ApiErrorResponse(HttpStatus.BAD_REQUEST,
                                                         getErrorCode(exception),
                                                         getMessage(ex));
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        violations.stream()
                  .map(constraintViolation -> {
                      ElementKind elementKind = getElementKindOfLastNode(constraintViolation.getPropertyPath());
                      if (elementKind == ElementKind.PROPERTY) {
                          return new ApiFieldError(getCode(constraintViolation),
                                                   constraintViolation.getPropertyPath().toString(),
                                                   getMessage(constraintViolation),
                                                   constraintViolation.getInvalidValue());
                      } else if (elementKind == ElementKind.BEAN) {
                          return new ApiGlobalError(getCode(constraintViolation),
                                                    getMessage(constraintViolation));
                      } else {
                          LOGGER.warn("Unable to convert constraint violation with element kind {}: {}", elementKind, constraintViolation);
                          return null;
                      }
                  })
                  .forEach(error -> {
                      if (error instanceof ApiFieldError) {
                          response.addFieldError((ApiFieldError) error);
                      } else if (error instanceof ApiGlobalError) {
                          response.addGlobalError((ApiGlobalError) error);
                      }
                  });

        return response;
    }

    private ElementKind getElementKindOfLastNode(Path path) {
        ElementKind result = null;
        for (Path.Node node : path) {
            result = node.getKind();
        }

        return result;
    }

    private String getCode(ConstraintViolation<?> constraintViolation) {
        String code = constraintViolation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
        String fieldSpecificCode = constraintViolation.getPropertyPath().toString() + "." + code;
        if (hasConfiguredOverrideForCode(fieldSpecificCode)) {
            return replaceCodeWithConfiguredOverrideIfPresent(fieldSpecificCode);
        }
        return replaceCodeWithConfiguredOverrideIfPresent(code);
    }

    private String getMessage(ConstraintViolation<?> constraintViolation) {
        String code = constraintViolation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
        String fieldSpecificCode = constraintViolation.getPropertyPath().toString() + "." + code;
        if (hasConfiguredOverrideForMessage(fieldSpecificCode)) {
            return getOverrideMessage(fieldSpecificCode);
        }

        if (hasConfiguredOverrideForMessage(code)) {
            return getOverrideMessage(code);
        }
        return getConstraintViolationMessage(constraintViolation);
    }

    private String getMessage(ConstraintViolationException exception) {
        String errorCode = ConstraintViolationException.class.getSimpleName();
        return getMessage(
            errorCode,
            new Object[] { exception.getConstraintViolations().size() },
            "Validation failed. Error count: " + exception.getConstraintViolations().size()
        );
    }

    private String getConstraintViolationMessage(ConstraintViolation<?> constraintViolation) {
        ConstraintDescriptor<?> cd = constraintViolation
                .getConstraintDescriptor();
        String errorCode = cd.getAnnotation().annotationType()
                .getSimpleName();
        Object[] errorArgs = getArgumentsForConstraint(cd);
        return getMessage(
                errorCode,
                errorArgs,
                constraintViolation.getMessage()
        );
    }

    /**
     * Returns all actual constraint annotation
     * attributes (i.e. excluding "message", "groups" and "payload") in
     * alphabetical order of their attribute names.
     */
    private Object[] getArgumentsForConstraint(ConstraintDescriptor<?> descriptor) {
        // Using a TreeMap for alphabetical ordering of attribute names
        Map<String, Object> attributesToExpose = new TreeMap<>();
        for (Map.Entry<String, Object> entry : descriptor.getAttributes()
                .entrySet()) {
            String attributeName = entry.getKey();
            Object attributeValue = entry.getValue();
            if (!internalAnnotationAttributes.contains(attributeName)) {
                attributesToExpose.put(attributeName, attributeValue);
            }
        }
        return attributesToExpose.values().toArray();
    }

}
