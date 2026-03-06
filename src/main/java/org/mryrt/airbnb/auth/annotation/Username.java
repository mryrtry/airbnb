package org.mryrt.airbnb.auth.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.mryrt.airbnb.auth.constants.UserConstants.USERNAME_MAX_LENGTH;
import static org.mryrt.airbnb.auth.constants.UserConstants.USERNAME_MIN_LENGTH;


@Constraint(validatedBy = UsernameValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@NotBlank(message = "User.username не может быть пустым")
@Size(min = USERNAME_MIN_LENGTH, max = USERNAME_MAX_LENGTH, message = "User.username должен быть от {min} до {max} символов")
@Pattern(regexp = "^[a-zA-Z0-9_\\s]+$", message = "User.username может содержать только буквы, цифры, нижние подчеркивания, пробельные символы")
public @interface Username {

    String message() default "Невалидный User.username";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}