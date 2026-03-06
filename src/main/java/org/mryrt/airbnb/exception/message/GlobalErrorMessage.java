package org.mryrt.airbnb.exception.message;

import lombok.Getter;
import org.mryrt.airbnb.exception.ErrorMessage;
import org.springframework.http.HttpStatus;


@Getter
public enum GlobalErrorMessage implements ErrorMessage {

	MUST_BE_NOT_NULL(HttpStatus.BAD_REQUEST, "Ресурс '%s' не может быть пустым"),
	ID_MUST_BE_POSITIVE(HttpStatus.BAD_REQUEST, "Идентификатор ресурса '%s' должен быть положительным"),
	SOURCE_WITH_ID_NOT_FOUND(HttpStatus.NOT_FOUND, "Ресурс '%s' с ID: '%s' не был найден"),
	SOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Ресурс '%s' не был найден"),
	CANNOT_ACCESS_SOURCE(HttpStatus.UNAUTHORIZED, "Ресурс '%s' с ID: '%s' не может быть удалён или изменён вами"),
	ENTITY_IN_USE(HttpStatus.BAD_REQUEST, "Невозможно удалить ресурс '%s' с ID: '%s', так как он связан с другим ресурсом '%s'"),
	MUST_BE_UNIQUE(HttpStatus.BAD_REQUEST, "Ресурс '%s' должен быть уникальным");


	private final HttpStatus httpStatus;

	private final String messageTemplate;

	GlobalErrorMessage(HttpStatus httpStatus, String messageTemplate) {
		this.httpStatus = httpStatus;
		this.messageTemplate = messageTemplate;
	}

	@Override
	public String getFormattedMessage(Object... args) {
		return messageTemplate.formatted(args);
	}

}
