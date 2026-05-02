package org.mryrt.airbnb.auth.service.user;

import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.auth.dto.UserDto;
import org.mryrt.airbnb.auth.dto.UserMapper;
import org.mryrt.airbnb.auth.dto.request.LoginRequest;
import org.mryrt.airbnb.auth.dto.request.RoleRequest;
import org.mryrt.airbnb.auth.dto.request.UserRequest;
import org.mryrt.airbnb.auth.dto.request.UserUpdateRequest;
import org.mryrt.airbnb.auth.exception.AuthCredNotValidException;
import org.mryrt.airbnb.auth.jaas.XmlCredentialsStore;
import org.mryrt.airbnb.auth.model.Permission;
import org.mryrt.airbnb.auth.model.Role;
import org.mryrt.airbnb.auth.model.User;
import org.mryrt.airbnb.auth.model.UserDetailsImpl;
import org.mryrt.airbnb.auth.repository.filter.UserFilter;
import org.mryrt.airbnb.event.EntityEvent;
import org.mryrt.airbnb.exception.ServiceException;
import org.mryrt.airbnb.util.pageable.PageableRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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

    private final XmlCredentialsStore xmlCredentialsStore;

    private final PasswordEncoder passwordEncoder;

    private final UserMapper mapper;

    private final ApplicationEventPublisher eventPublisher;

    private User findUser(Long id) {
        if (id == null) throw new ServiceException(MUST_BE_NOT_NULL, "User.id");
        if (id <= 0) throw new ServiceException(ID_MUST_BE_POSITIVE, "User.id");
        return xmlCredentialsStore.findById(id)
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "User", id));
    }

    private User findUser(String username) {
        if (username == null || username.isBlank()) throw new ServiceException(MUST_BE_NOT_NULL, "User.username");
        return xmlCredentialsStore.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        SOURCE_WITH_ID_NOT_FOUND.getFormattedMessage("User", username)));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return new UserDetailsImpl(findUser(username));
    }

    @Override
    public UserDto create(UserRequest request) {
        return createWithRoles(request, Set.of(Role.ROLE_USER));
    }

    @Override
    public UserDto createOwner(UserRequest request) {
        return createWithRoles(request, Set.of(Role.ROLE_USER, Role.ROLE_OWNER));
    }

    private UserDto createWithRoles(UserRequest request, Set<Role> roles) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(roles);
        xmlCredentialsStore.saveUser(user);
        UserDto createdUser = mapper.toDto(findUser(user.getUsername()));
        eventPublisher.publishEvent(new EntityEvent<>(CREATED, createdUser));
        return createdUser;
    }

    @Override
    public Page<UserDto> getAll(UserFilter filter, PageableRequest config) {
        List<User> all = xmlCredentialsStore.findAll();

        Stream<User> stream = all.stream();
        if (filter != null) {
            if (filter.getUsername() != null) {
                stream = stream.filter(u -> u.getUsername().contains(filter.getUsername()));
            }
            if (filter.getCreatedAtAfter() != null) {
                stream = stream.filter(u -> u.getCreatedAt() != null &&
                        !u.getCreatedAt().toLocalDate().isBefore(filter.getCreatedAtAfter()));
            }
            if (filter.getCreatedAtBefore() != null) {
                stream = stream.filter(u -> u.getCreatedAt() != null &&
                        !u.getCreatedAt().toLocalDate().isAfter(filter.getCreatedAtBefore()));
            }
            if (filter.getUpdatedAtAfter() != null) {
                stream = stream.filter(u -> u.getUpdatedAt() != null &&
                        !u.getUpdatedAt().toLocalDate().isBefore(filter.getUpdatedAtAfter()));
            }
            if (filter.getUpdatedAtBefore() != null) {
                stream = stream.filter(u -> u.getUpdatedAt() != null &&
                        !u.getUpdatedAt().toLocalDate().isAfter(filter.getUpdatedAtBefore()));
            }
        }

        List<User> filtered = stream
                .sorted(Comparator.comparing(User::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        int page = config != null ? config.getPage() : 0;
        int size = config != null ? config.getSize() : 20;
        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        List<User> pageContent = start >= filtered.size() ? List.of() : filtered.subList(start, end);

        return new PageImpl<>(
                pageContent.stream().map(mapper::toDto).toList(),
                PageRequest.of(page, size),
                filtered.size()
        );
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
        return mapper.toDto(findUser(authentication.getName()));
    }

    @Override
    public User getEntity(Long id) {
        return findUser(id);
    }

    @Override
    public UserDto update(Long id, UserUpdateRequest request) {
        User user = findUser(id);
        if (request.getUsername() != null) user.setUsername(request.getUsername());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPassword() != null) user.setPassword(passwordEncoder.encode(request.getPassword()));
        xmlCredentialsStore.saveUser(user);
        UserDto updatedUser = mapper.toDto(user);
        eventPublisher.publishEvent(new EntityEvent<>(UPDATED, updatedUser));
        return updatedUser;
    }

    @Override
    public UserDto updateRoles(Long id, RoleRequest request) {
        User user = findUser(id);
        user.setRoles(request.getRoles());
        xmlCredentialsStore.saveUser(user);
        UserDto updatedUser = mapper.toDto(user);
        eventPublisher.publishEvent(new EntityEvent<>(UPDATED, updatedUser));
        return updatedUser;
    }

    @Override
    public UserDto delete(Long id) {
        User user = findUser(id);
        xmlCredentialsStore.removeUser(user.getUsername());
        UserDto deletedUser = mapper.toDto(user);
        eventPublisher.publishEvent(new EntityEvent<>(DELETED, user));
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
