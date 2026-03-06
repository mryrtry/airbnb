package org.mryrt.airbnb.auth.exception.message;

import lombok.Getter;
import org.mryrt.airbnb.exception.ErrorMessage;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorMessages implements ErrorMessage {

    UNEXPECTED_AUTH_EXCEPTION(HttpStatus.UNAUTHORIZED, "Ошибка в ходе авторизации"),
    EXPIRED_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Права пользователя истекли"),
    DEAD_ACCOUNT(HttpStatus.UNAUTHORIZED, "Аккаунт пользователя неактивен"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Некорректный токен"),
    INCORRECT_PASSWORD(HttpStatus.UNAUTHORIZED, "Неверный пароль, попробуйте ещё раз"),
    USER_NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED, "Пользователь не авторизован"),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "Токен в черном списке");


    private final HttpStatus httpStatus;

    private final String messageTemplate;

    AuthErrorMessages(HttpStatus httpStatus, String messageTemplate) {
        this.httpStatus = httpStatus;
        this.messageTemplate = messageTemplate;
    }

    @Override
    public String getFormattedMessage(Object... args) {
        return messageTemplate.formatted(args);
    }

}
