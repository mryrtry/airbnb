package org.mryrt.airbnb.auth.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.mryrt.airbnb.auth.jaas.XmlCredentialsStore;
import org.springframework.beans.factory.annotation.Autowired;


public class LoginValidator implements ConstraintValidator<Login, String> {

    @Autowired
    private XmlCredentialsStore xmlCredentialsStore;

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (!xmlCredentialsStore.existsByUsername(username)) {
            context.buildConstraintViolationWithTemplate("Пользователь с именем '%s' не найден".formatted(username))
                    .addConstraintViolation()
                    .disableDefaultConstraintViolation();
            return false;
        }
        return true;
    }

}