package org.mryrt.airbnb.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public class ServiceException extends RuntimeException {

    private final HttpStatus httpStatus;

    public ServiceException(ErrorMessage errorMessage, Object... args) {
        super(errorMessage.getFormattedMessage(args));
        this.httpStatus = HttpStatus.valueOf(errorMessage.getHttpStatus().value());
    }

}
