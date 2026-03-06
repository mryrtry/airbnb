package org.mryrt.airbnb.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mryrt.airbnb.auth.model.Role;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;

    private String username;

    private String email;

    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

	private Long version;

}
