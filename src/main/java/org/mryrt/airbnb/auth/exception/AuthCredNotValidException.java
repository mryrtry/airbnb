package org.mryrt.airbnb.auth.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class AuthCredNotValidException extends RuntimeException {

    Map<String, String> details;

    public AuthCredNotValidException(Map<String, String> fieldErrors) {
        super("Ошибка в ходе авторизации");
        this.details = fieldErrors;
    }

}
