package org.mryrt.airbnb.notification.service;

import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.auth.repository.UserRepository;
import org.mryrt.airbnb.exception.ServiceException;
import org.mryrt.airbnb.notification.dto.NotificationDto;
import org.mryrt.airbnb.notification.dto.NotificationMapper;
import org.mryrt.airbnb.notification.model.Notification;
import org.mryrt.airbnb.notification.model.NotificationType;
import org.mryrt.airbnb.notification.repository.NotificationRepository;
import org.mryrt.airbnb.notification.message.NotificationContent;
import org.mryrt.airbnb.notification.message.NotificationMessageProvider;
import org.mryrt.airbnb.notification.repository.filter.NotificationFilter;
import org.mryrt.airbnb.notification.service.email.EmailService;
import org.mryrt.airbnb.util.pageable.PageableFactory;
import org.mryrt.airbnb.util.pageable.PageableRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.ID_MUST_BE_POSITIVE;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.MUST_BE_NOT_NULL;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.SOURCE_WITH_ID_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper mapper;
    private final PageableFactory pageableFactory;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final NotificationMessageProvider messageProvider;

    @Override
    @Transactional
    public void notifyUser(Long userId, NotificationType type, Map<String, Object> context) {
        NotificationContent content = messageProvider.getContent(type, context);
        Long relatedBookingId = context != null && context.containsKey("bookingId") ? (Long) context.get("bookingId") : null;
        Long relatedResolutionId = context != null && context.containsKey("resolutionId") ? (Long) context.get("resolutionId") : null;
        notifyUser(userId, type, content.getTitle(), content.getBody(), relatedBookingId, relatedResolutionId);
    }

    @Override
    @Transactional
    public void notifyUser(Long userId, NotificationType type, String title, String body) {
        notifyUser(userId, type, title, body, null, null);
    }

    private void notifyUser(Long userId, NotificationType type, String title, String body, Long relatedBookingId, Long relatedResolutionId) {
        Notification n = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body != null ? body : "")
                .relatedBookingId(relatedBookingId)
                .relatedResolutionId(relatedResolutionId)
                .read(false)
                .build();
        notificationRepository.save(n);
        userRepository.findById(userId)
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .ifPresent(u -> emailService.send(u.getEmail(), title, body != null ? body : title));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getAll(NotificationFilter filter, PageableRequest pageable) {
        Pageable page = pageableFactory.create(pageable, Notification.class);
        return notificationRepository.findWithFilter(filter, page).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDto get(Long id) {
        return mapper.toDto(findNotification(id));
    }

    @Override
    @Transactional
    public NotificationDto markRead(Long id) {
        Notification n = findNotification(id);
        n.setRead(true);
        n.setReadAt(LocalDateTime.now());
        return mapper.toDto(notificationRepository.save(n));
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        NotificationFilter filter = NotificationFilter.builder().userId(userId).unreadOnly(true).build();
        Page<Notification> page = notificationRepository.findWithFilter(filter, Pageable.ofSize(500));
        page.getContent().forEach(n -> {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
        });
        while (page.hasNext()) {
            page = notificationRepository.findWithFilter(filter, page.nextPageable());
            page.getContent().forEach(n -> {
                n.setRead(true);
                n.setReadAt(LocalDateTime.now());
                notificationRepository.save(n);
            });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.findWithFilter(
                NotificationFilter.builder().userId(userId).unreadOnly(true).build(),
                Pageable.unpaged()
        ).getTotalElements();
    }

    private Notification findNotification(Long id) {
        if (id == null) throw new ServiceException(MUST_BE_NOT_NULL, "Notification.id");
        if (id <= 0) throw new ServiceException(ID_MUST_BE_POSITIVE, "Notification.id");
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "Notification", id));
    }
}
