package org.mryrt.airbnb.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mryrt.airbnb.auth.annotation.Username;
import org.mryrt.airbnb.auth.constants.UserConstants;

import jakarta.validation.constraints.Email;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    @Username
    private String username;

    @Email
    @Size(max = UserConstants.EMAIL_MAX_LENGTH)
    private String email;

    @NotBlank
    @Size(min = UserConstants.PASSWORD_MIN_LENGTH, message = "User.password должен быть длиннее {min} символов")
    private String password;

    public void clearPassword() {
        this.password = null;
    }

}
