package org.mryrt.airbnb.notification.controller;

import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.auth.service.user.UserService;
import org.mryrt.airbnb.notification.dto.NotificationDto;
import org.mryrt.airbnb.notification.repository.filter.NotificationFilter;
import org.mryrt.airbnb.notification.service.NotificationService;
import org.mryrt.airbnb.util.pageable.PageableRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API уведомлений в личном кабинете: список (все/только непрочитанные), отметка прочитано, счётчик непрочитанных.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    /** Список уведомлений текущего пользователя (опционально только непрочитанные). */
    @GetMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_READ') or hasAuthority('ALL_NOTIFICATION_READ')")
    public ResponseEntity<Page<NotificationDto>> getAll(
            @ModelAttribute NotificationFilter filter,
            @ModelAttribute PageableRequest pageable,
            @RequestParam(required = false) Boolean unreadOnly) {
        Long currentUserId = userService.getAuthenticatedUser().getId();
        NotificationFilter f = filter == null ? new NotificationFilter() : filter;
        if (f.getUserId() == null) f.setUserId(currentUserId);
        if (f.getUserId() != null && !f.getUserId().equals(currentUserId) && !userService.getPermissions().contains("ALL_NOTIFICATION_READ")) {
            f.setUserId(currentUserId);
        }
        if (unreadOnly != null) f.setUnreadOnly(unreadOnly);
        return ResponseEntity.ok(notificationService.getAll(f, pageable));
    }

    /** Количество непрочитанных уведомлений текущего пользователя. */
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('NOTIFICATION_READ') or hasAuthority('ALL_NOTIFICATION_READ')")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        Long userId = userService.getAuthenticatedUser().getId();
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(userId)));
    }

    /** Одно уведомление по id (своё или любое при ALL_NOTIFICATION_READ). */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('NOTIFICATION_READ') or hasAuthority('ALL_NOTIFICATION_READ')")
    public ResponseEntity<NotificationDto> get(@PathVariable Long id) {
        NotificationDto dto = notificationService.get(id);
        Long currentUserId = userService.getAuthenticatedUser().getId();
        if (!dto.getUserId().equals(currentUserId) && !userService.getPermissions().contains("ALL_NOTIFICATION_READ")) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(dto);
    }

    /** Отметить уведомление как прочитанное. */
    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAuthority('NOTIFICATION_READ') or hasAuthority('ALL_NOTIFICATION_READ')")
    public ResponseEntity<NotificationDto> markRead(@PathVariable Long id) {
        NotificationDto dto = notificationService.get(id);
        Long currentUserId = userService.getAuthenticatedUser().getId();
        if (!dto.getUserId().equals(currentUserId) && !userService.getPermissions().contains("ALL_NOTIFICATION_READ")) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(notificationService.markRead(id));
    }

    /** Отметить все уведомления текущего пользователя как прочитанные. */
    @PostMapping("/mark-all-read")
    @PreAuthorize("hasAuthority('NOTIFICATION_READ') or hasAuthority('ALL_NOTIFICATION_READ')")
    public ResponseEntity<Void> markAllRead() {
        Long userId = userService.getAuthenticatedUser().getId();
        notificationService.markAllRead(userId);
        return ResponseEntity.ok().build();
    }
}
