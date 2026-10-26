package edu.kle.permits;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordByteValidator implements ConstraintValidator<ValidPassword,String> {
 public boolean isValid(String value,ConstraintValidatorContext context){return value!=null&&Passwords.valid(value);}
}
