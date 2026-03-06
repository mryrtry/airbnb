package org.mryrt.airbnb.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mryrt.airbnb.auth.model.Role;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequest {

    @NotNull(message = "User.roles не может быть пустым")
    private Set<Role> roles;

}