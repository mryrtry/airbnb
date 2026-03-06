package org.mryrt.airbnb.auth.service.user;

import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.auth.dto.UserDto;
import org.mryrt.airbnb.auth.dto.UserMapper;
import org.mryrt.airbnb.auth.dto.request.LoginRequest;
import org.mryrt.airbnb.auth.dto.request.RoleRequest;
import org.mryrt.airbnb.auth.dto.request.UserRequest;
import org.mryrt.airbnb.auth.dto.request.UserUpdateRequest;
import org.mryrt.airbnb.auth.exception.AuthCredNotValidException;
import org.mryrt.airbnb.auth.model.Permission;
import org.mryrt.airbnb.auth.model.Role;
import org.mryrt.airbnb.auth.model.User;
import org.mryrt.airbnb.auth.model.UserDetailsImpl;
import org.mryrt.airbnb.auth.repository.UserRepository;
import org.mryrt.airbnb.auth.repository.filter.UserFilter;
import org.mryrt.airbnb.event.EntityEvent;
import org.mryrt.airbnb.exception.ServiceException;
import org.mryrt.airbnb.util.pageable.PageableFactory;
import org.mryrt.airbnb.util.pageable.PageableRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mryrt.airbnb.auth.exception.message.AuthErrorMessages.INCORRECT_PASSWORD;
import static org.mryrt.airbnb.auth.exception.message.AuthErrorMessages.USER_NOT_AUTHENTICATED;
import static org.mryrt.airbnb.event.EventType.CREATED;
import static org.mryrt.airbnb.event.EventType.DELETED;
import static org.mryrt.airbnb.event.EventType.UPDATED;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.ID_MUST_BE_POSITIVE;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.MUST_BE_NOT_NULL;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.SOURCE_WITH_ID_NOT_FOUND;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final UserMapper mapper;

    private final ApplicationEventPublisher eventPublisher;

    private final PageableFactory pageableFactory;

    private User findUser(Long id) {
        if (id == null) {
            throw new ServiceException(MUST_BE_NOT_NULL, "User.id");
        }
        if (id <= 0) {
            throw new ServiceException(ID_MUST_BE_POSITIVE, "User.id");
        }
        return userRepository.findById(id).orElseThrow(() ->
                new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "User", id));
    }

    private User findUser(String username) {
        if (username == null || username.isBlank()) {
            throw new ServiceException(MUST_BE_NOT_NULL, "User.username");
        }
        return userRepository.findByUsername(username).orElseThrow(() ->
                new UsernameNotFoundException(SOURCE_WITH_ID_NOT_FOUND.getFormattedMessage("User", username))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return new UserDetailsImpl(findUser(username));
    }

    @Override
    @Transactional
    public UserDto create(UserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(Role.ROLE_USER));
        UserDto createdUser = mapper.toDto(userRepository.save(user));
        eventPublisher.publishEvent(new EntityEvent<>(CREATED, createdUser));
        return createdUser;
    }

    @Override
    public Page<UserDto> getAll(UserFilter filter, PageableRequest config) {
        Pageable pageable = pageableFactory.create(config, User.class);
        Page<User> users = userRepository.findWithFilter(filter, pageable);
        return users.map(mapper::toDto);
    }

    @Override
    public UserDto get(Long id) {
        return mapper.toDto(findUser(id));
    }

    @Override
    public UserDto get(String username) {
        return mapper.toDto(findUser(username));
    }

    @Override
    public List<String> getPermissions() {
        return getAuthenticatedUser().getRoles()
                .stream()
                .flatMap(role -> role.getPermissions().stream().map(Object::toString))
                .toList();
    }

    @Override
    public UserDto getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            throw new ServiceException(USER_NOT_AUTHENTICATED);
        }
        String username = authentication.getName();
        User user = findUser(username);
        return mapper.toDto(user);
    }

    @Override
    public User getEntity(Long id) {
        return findUser(id);
    }

    @Override
    @Transactional
    public UserDto update(Long id, UserUpdateRequest request) {
        User updatingUser = findUser(id);
        updatingUser.setUsername(request.getUsername());
        if (request.getEmail() != null) updatingUser.setEmail(request.getEmail());
        if (request.getPassword() != null) {
			updatingUser.setPassword(passwordEncoder.encode(request.getPassword()));
		}
        UserDto updatedUser = mapper.toDto(userRepository.save(updatingUser));
        eventPublisher.publishEvent(new EntityEvent<>(UPDATED, updatedUser));
        return updatedUser;
    }

    @Override
    @Transactional
    public UserDto updateRoles(Long id, RoleRequest request) {
        User updatingUser = findUser(id);
        updatingUser.setRoles(request.getRoles());
        UserDto updatedUser = mapper.toDto(userRepository.save(updatingUser));
        eventPublisher.publishEvent(new EntityEvent<>(UPDATED, updatedUser));
        return updatedUser;
    }

    @Override
    @Transactional
    public UserDto delete(Long id) {
        User deletingUser = findUser(id);
        userRepository.delete(deletingUser);
        UserDto deletedUser = mapper.toDto(deletingUser);
        eventPublisher.publishEvent(new EntityEvent<>(DELETED, deletingUser));
        return deletedUser;
    }

    @Override
    public void validateLogin(LoginRequest loginRequest) {
        User user = findUser(loginRequest.getUsername());
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword()))
            throw new AuthCredNotValidException(Map.of("password", INCORRECT_PASSWORD.getFormattedMessage()));
    }

    @Override
    public boolean authenticatedUserHasPermission(Permission permission) {
        return getAuthenticatedUser()
                .getRoles()
                .stream()
                .anyMatch(role -> role.getPermissions().contains(permission));
    }

}
