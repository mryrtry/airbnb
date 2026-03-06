package org.mryrt.airbnb.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mryrt.airbnb.auth.annotation.Login;
import org.mryrt.airbnb.auth.constants.UserConstants;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Login
    private String username;

    @NotBlank
    @Size(min = UserConstants.PASSWORD_MIN_LENGTH, message = "User.password должен быть длиннее {min} символов")
    private String password;

    public void clearPassword() {
        this.password = null;
    }

}
