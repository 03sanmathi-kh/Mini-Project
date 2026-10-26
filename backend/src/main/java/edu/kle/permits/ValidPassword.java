package edu.kle.permits;

import jakarta.validation.*;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy=PasswordByteValidator.class)
@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.RECORD_COMPONENT,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
 String message() default "must be 8–72 UTF-8 bytes";
 Class<?>[] groups() default {};
 Class<? extends Payload>[] payload() default {};
}
