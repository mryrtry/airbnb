package org.mryrt.airbnb.auth.service.user;

import jakarta.validation.Valid;
import org.mryrt.airbnb.auth.dto.UserDto;
import org.mryrt.airbnb.auth.dto.request.LoginRequest;
import org.mryrt.airbnb.auth.dto.request.RoleRequest;
import org.mryrt.airbnb.auth.dto.request.UserRequest;
import org.mryrt.airbnb.auth.dto.request.UserUpdateRequest;
import org.mryrt.airbnb.auth.model.Permission;
import org.mryrt.airbnb.auth.model.User;
import org.mryrt.airbnb.auth.repository.filter.UserFilter;
import org.mryrt.airbnb.util.pageable.PageableRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserService extends UserDetailsService {

    UserDto create(@Valid UserRequest request);

    Page<UserDto> getAll(UserFilter filter, PageableRequest config);

    UserDto get(Long id);

    UserDto get(String username);

    List<String> getPermissions();

    UserDto getAuthenticatedUser();

    User getEntity(Long id);

    UserDto update(Long id, @Valid UserUpdateRequest request);

    UserDto updateRoles(Long id, @Valid RoleRequest request);

    UserDto delete(Long id);

    void validateLogin(@Valid LoginRequest loginRequest);

    boolean authenticatedUserHasPermission(Permission permission);

}