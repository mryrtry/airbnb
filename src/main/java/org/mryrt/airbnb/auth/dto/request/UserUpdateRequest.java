package org.mryrt.airbnb.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mryrt.airbnb.auth.constants.UserConstants;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

	@Size(min = UserConstants.USERNAME_MIN_LENGTH, max = UserConstants.USERNAME_MAX_LENGTH, message = "User.username должен быть от {min} до {max} символов")
	@Pattern(regexp = "^[a-zA-Z0-9_\\s]+$", message = "User.username может содержать только буквы, цифры, нижние подчеркивания, пробельные символы")
	private String username;

	@Email(message = "User.email должен быть корректным адресом")
	@Size(max = UserConstants.EMAIL_MAX_LENGTH)
	private String email;

	@Size(min = UserConstants.PASSWORD_MIN_LENGTH, message = "User.password должен быть длиннее {min} символов")
	private String password;

}
