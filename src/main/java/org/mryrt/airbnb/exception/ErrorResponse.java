package org.mryrt.airbnb.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

	private int status;

	private String message;

	private List<ErrorDetail> details;

	private LocalDateTime timestamp;

	@Data
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ErrorDetail {

		private String field;

		private String message;

		private Object rejectedValue;

		private String errorType;

	}

}