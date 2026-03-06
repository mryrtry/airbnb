package org.mryrt.airbnb.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mryrt.airbnb.auth.dto.UserDto;
import org.mryrt.airbnb.auth.dto.request.RoleRequest;
import org.mryrt.airbnb.auth.dto.request.UserUpdateRequest;
import org.mryrt.airbnb.auth.repository.filter.UserFilter;
import org.mryrt.airbnb.auth.service.user.UserService;
import org.mryrt.airbnb.util.pageable.PageableRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API управления пользователями (список, текущий пользователь, права, обновление, роли, удаление).
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Возвращает страницу пользователей с фильтром и сортировкой. */
    @GetMapping
    @PreAuthorize("hasAuthority('ALL_USER_READ')")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @ModelAttribute @Valid UserFilter filter,
            @ModelAttribute PageableRequest config) {
        Page<UserDto> users = userService.getAll(filter, config);
        return ResponseEntity.ok(users);
    }

    /** Возвращает данные текущего аутентифицированного пользователя. */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getCurrentUser() {
        UserDto user = userService.getAuthenticatedUser();
        return ResponseEntity.ok(user);
    }

    /** Возвращает список прав текущего пользователя. */
    @GetMapping("/permissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getPermissions() {
        List<String> permissions = userService.getPermissions();
        return ResponseEntity.ok(permissions);
    }

    /** Возвращает пользователя по id. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ALL_USER_READ')")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        UserDto user = userService.get(id);
        return ResponseEntity.ok(user);
    }

    /** Возвращает пользователя по имени. */
    @GetMapping("/username/{username}")
    @PreAuthorize("hasAuthority('ALL_USER_READ')")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        UserDto user = userService.get(username);
        return ResponseEntity.ok(user);
    }

    /** Обновляет профиль пользователя (имя, пароль). */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ALL_USER_UPDATE')")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        UserDto updatedUser = userService.update(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    /** Обновляет роли пользователя. */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ALL_USER_UPDATE')")
    public ResponseEntity<UserDto> updateUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest request) {
        UserDto updatedUser = userService.updateRoles(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    /** Удаляет пользователя. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ALL_USER_DELETE')")
    public ResponseEntity<UserDto> deleteUser(@PathVariable Long id) {
        UserDto deletedUser = userService.delete(id);
        return ResponseEntity.ok(deletedUser);
    }
}