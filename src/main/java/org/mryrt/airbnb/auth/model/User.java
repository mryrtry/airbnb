package org.mryrt.airbnb.auth.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.mryrt.airbnb.auth.constants.UserConstants;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.mryrt.airbnb.auth.constants.UserConstants.USERNAME_MAX_LENGTH;
import static org.mryrt.airbnb.auth.constants.UserConstants.USERNAME_MIN_LENGTH;


@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "User.username не может быть пустым")
    @Size(min = USERNAME_MIN_LENGTH, max = USERNAME_MAX_LENGTH, message = "User.username должен быть от {min} до {max} символов")
    @Column(unique = true, nullable = false)
    private String username;

    @Email(message = "User.email должен быть корректным адресом")
    @Size(max = UserConstants.EMAIL_MAX_LENGTH)
    @Column(length = UserConstants.EMAIL_MAX_LENGTH)
    private String email;

    @NotBlank(message = "User.password не может быть пустым")
    @Size(min = UserConstants.PASSWORD_MIN_LENGTH, message = "User.password должен быть не менее {min} символов")
    @Column(nullable = false)
    private String password;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role"})
    )
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

}
