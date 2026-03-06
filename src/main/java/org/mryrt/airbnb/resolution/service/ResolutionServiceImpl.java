package org.mryrt.airbnb.resolution.service;

import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.auth.service.user.UserService;
import org.mryrt.airbnb.booking.model.Booking;
import org.mryrt.airbnb.booking.model.BookingStatus;
import org.mryrt.airbnb.booking.repository.BookingRepository;
import org.mryrt.airbnb.booking.service.BookingService;
import org.mryrt.airbnb.exception.ServiceException;
import org.mryrt.airbnb.listing.model.Listing;
import org.mryrt.airbnb.listing.service.ListingService;
import org.mryrt.airbnb.notification.model.NotificationType;
import org.mryrt.airbnb.notification.service.NotificationService;
import org.mryrt.airbnb.resolution.access.ResolutionAccessService;
import org.mryrt.airbnb.resolution.constants.ResolutionConstants;
import org.mryrt.airbnb.resolution.dto.ResolutionDto;
import org.mryrt.airbnb.resolution.dto.ResolutionMapper;
import org.mryrt.airbnb.resolution.dto.request.ComplaintRequest;
import org.mryrt.airbnb.resolution.dto.request.RequestMoneyRequest;
import org.mryrt.airbnb.resolution.dto.request.ResolveEscalationRequest;
import org.mryrt.airbnb.resolution.model.ResolutionStatus;
import org.mryrt.airbnb.resolution.model.ResolutionWindow;
import org.mryrt.airbnb.resolution.repository.ResolutionRepository;
import org.mryrt.airbnb.resolution.repository.filter.ResolutionFilter;
import org.mryrt.airbnb.util.pageable.PageableFactory;
import org.mryrt.airbnb.util.pageable.PageableRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.ID_MUST_BE_POSITIVE;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.MUST_BE_NOT_NULL;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.SOURCE_WITH_ID_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ResolutionServiceImpl implements ResolutionService {

    private final ResolutionRepository resolutionRepository;
    private final ResolutionMapper mapper;
    private final ResolutionAccessService resolutionAccessService;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final ListingService listingService;
    private final PageableFactory pageableFactory;
    private final NotificationService notificationService;
    private final UserService userService;

    private ResolutionWindow findWindow(Long id) {
        if (id == null) throw new ServiceException(MUST_BE_NOT_NULL, "ResolutionWindow.id");
        if (id <= 0) throw new ServiceException(ID_MUST_BE_POSITIVE, "ResolutionWindow.id");
        return resolutionRepository.findById(id)
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "ResolutionWindow", id));
    }

    @Override
    @Transactional
    public ResolutionDto openForBooking(Long bookingId) {
        Booking booking = bookingService.getEntity(bookingId);
        if (booking.getStatus() != BookingStatus.CHECKED_OUT) {
            throw new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "Booking (must be CHECKED_OUT)", bookingId);
        }
        var existing = resolutionRepository.findByBookingId(bookingId);
        if (existing.isPresent()) {
            return mapper.toDto(existing.get());
        }
        ResolutionWindow window = ResolutionWindow.builder()
                .bookingId(bookingId)
                .status(ResolutionStatus.OPEN)
                .openedAt(LocalDateTime.now())
                .build();
        window = resolutionRepository.save(window);
        Listing listing = listingService.getEntity(booking.getListingId());
        notificationService.notifyUser(listing.getOwnerId(), NotificationType.RESOLUTION_WINDOW_OPENED, Map.of("bookingId", bookingId, "resolutionId", window.getId()));
        return mapper.toDto(window);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResolutionDto> getAll(ResolutionFilter filter, PageableRequest pageable) {
        Pageable page = pageableFactory.create(pageable, ResolutionWindow.class);
        List<Long> scopeBookingIds = resolutionAccessService.getScopeBookingIdsForList();
        return resolutionRepository.findWithFilter(filter, page, scopeBookingIds).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ResolutionDto get(Long id) {
        resolutionAccessService.requireCanRead(id);
        return mapper.toDto(findWindow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ResolutionDto getByBookingId(Long bookingId) {
        ResolutionWindow window = resolutionRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "ResolutionWindow", bookingId));
        resolutionAccessService.requireCanRead(window.getId());
        return mapper.toDto(window);
    }

    @Override
    @Transactional
    public ResolutionDto requestMoney(Long id, RequestMoneyRequest request) {
        resolutionAccessService.requireIsOwner(id);
        ResolutionWindow window = findWindow(id);
        if (window.getStatus() != ResolutionStatus.OPEN) {
            throw new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "ResolutionWindow (must be OPEN)", id);
        }
        window.setStatus(ResolutionStatus.MONEY_REQUESTED);
        window.setAmountRequested(request.getAmountRequested());
        window.setMoneyRequestedAt(LocalDateTime.now());
        window = resolutionRepository.save(window);
        Booking b = bookingService.getEntity(window.getBookingId());
        notificationService.notifyUser(b.getGuestId(), NotificationType.RESOLUTION_MONEY_REQUESTED,
                Map.of("bookingId", window.getBookingId(), "amount", request.getAmountRequested(), "resolutionId", window.getId()));
        return mapper.toDto(window);
    }

    @Override
    @Transactional
    public ResolutionDto respondPay(Long id) {
        resolutionAccessService.requireIsGuest(id);
        ResolutionWindow window = findWindow(id);
        if (window.getStatus() != ResolutionStatus.MONEY_REQUESTED && window.getStatus() != ResolutionStatus.MANDATORY_PAYMENT) {
            throw new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "ResolutionWindow (must be MONEY_REQUESTED or MANDATORY_PAYMENT)", id);
        }
        window.setStatus(ResolutionStatus.PAID);
        window.setClosedAt(LocalDateTime.now());
        window = resolutionRepository.save(window);
        Listing listing = listingService.getEntity(bookingService.getEntity(window.getBookingId()).getListingId());
        notificationService.notifyUser(listing.getOwnerId(), NotificationType.RESOLUTION_PAYMENT_RECEIVED, Map.of("bookingId", window.getBookingId(), "resolutionId", window.getId()));
        return mapper.toDto(window);
    }

    @Override
    @Transactional
    public ResolutionDto respondRefuse(Long id) {
        resolutionAccessService.requireIsGuest(id);
        ResolutionWindow window = findWindow(id);
        if (window.getStatus() != ResolutionStatus.MONEY_REQUESTED) {
            throw new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "ResolutionWindow (must be MONEY_REQUESTED)", id);
        }
        window.setStatus(ResolutionStatus.REFUSED);
        window.setRefusedAt(LocalDateTime.now());
        window = resolutionRepository.save(window);
        Listing listing = listingService.getEntity(bookingService.getEntity(window.getBookingId()).getListingId());
        notificationService.notifyUser(listing.getOwnerId(), NotificationType.RESOLUTION_GUEST_REFUSED, Map.of("bookingId", window.getBookingId(), "resolutionId", window.getId()));
        return mapper.toDto(window);
    }

    @Override
    @Transactional
    public ResolutionDto escalate(Long id) {
        resolutionAccessService.requireIsOwner(id);
        ResolutionWindow window = findWindow(id);
        if (window.getStatus() != ResolutionStatus.REFUSED) {
            throw new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "ResolutionWindow (must be REFUSED)", id);
        }
        window.setStatus(ResolutionStatus.ESCALATED);
        window = resolutionRepository.save(window);
        Booking b = bookingService.getEntity(window.getBookingId());
        notificationService.notifyUser(b.getGuestId(), NotificationType.RESOLUTION_ESCALATED, Map.of("bookingId", window.getBookingId(), "resolutionId", window.getId()));
        return mapper.toDto(window);
    }

    @Override
    @Transactional
    public ResolutionDto recordComplaint(Long id, ComplaintRequest request) {
        resolutionAccessService.requireIsOwner(id);
        ResolutionWindow window = findWindow(id);
        if (window.getStatus() != ResolutionStatus.OPEN) {
            throw new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "ResolutionWindow (must be OPEN)", id);
        }
        window.setStatus(ResolutionStatus.COMPLAINT_RECORDED);
        window.setComplaintDescription(request.getDescription());
        window.setClosedAt(LocalDateTime.now());
        window = resolutionRepository.save(window);
        Listing listing = listingService.getEntity(bookingService.getEntity(window.getBookingId()).getListingId());
        notificationService.notifyUser(listing.getOwnerId(), NotificationType.RESOLUTION_COMPLAINT_RECORDED, Map.of("bookingId", window.getBookingId(), "resolutionId", window.getId()));
        return mapper.toDto(window);
    }

    @Override
    @Transactional
    public ResolutionDto close(Long id) {
        resolutionAccessService.requireCanClose(id);
        ResolutionWindow window = findWindow(id);
        ResolutionStatus previousStatus = window.getStatus();
        Listing listing = listingService.getEntity(bookingService.getEntity(window.getBookingId()).getListingId());
        boolean closedByOwnerWithoutEscalation = previousStatus == ResolutionStatus.REFUSED
                && listing.getOwnerId().equals(userService.getAuthenticatedUser().getId());
        window.setStatus(ResolutionStatus.CLOSED);
        window.setClosedAt(LocalDateTime.now());
        window = resolutionRepository.save(window);
        NotificationType closeType = closedByOwnerWithoutEscalation ? NotificationType.RESOLUTION_WINDOW_CLOSED_BY_OWNER : NotificationType.RESOLUTION_WINDOW_CLOSED;
        notificationService.notifyUser(listing.getOwnerId(), closeType, Map.of("bookingId", window.getBookingId(), "resolutionId", window.getId()));
        return mapper.toDto(window);
    }

    @Override
    @Transactional
    public ResolutionDto resolveEscalation(Long id, ResolveEscalationRequest request) {
        resolutionAccessService.requireCanResolveEscalation(id);
        ResolutionWindow window = findWindow(id);
        if (window.getStatus() != ResolutionStatus.ESCALATED) {
            throw new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "ResolutionWindow (must be ESCALATED)", id);
        }
        if (Boolean.TRUE.equals(request.getSubstantialIncident())) {
            window.setStatus(ResolutionStatus.MANDATORY_PAYMENT);
            window = resolutionRepository.save(window);
            Booking b = bookingService.getEntity(window.getBookingId());
            notificationService.notifyUser(b.getGuestId(), NotificationType.RESOLUTION_MANDATORY_PAYMENT_ASSIGNED,
                    Map.of("bookingId", window.getBookingId(), "amount", window.getAmountRequested(), "resolutionId", window.getId()));
        } else {
            window.setStatus(ResolutionStatus.CLOSED);
            window.setClosedAt(LocalDateTime.now());
            window = resolutionRepository.save(window);
            Listing listing = listingService.getEntity(bookingService.getEntity(window.getBookingId()).getListingId());
            notificationService.notifyUser(listing.getOwnerId(), NotificationType.RESOLUTION_RESOLVED_WITHOUT_PAYMENT,
                    Map.of("bookingId", window.getBookingId(), "resolutionId", window.getId()));
        }
        return mapper.toDto(window);
    }

    @Override
    @Transactional
    public void closeExpiredWindows() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(ResolutionConstants.WINDOW_DAYS);
        var expiredOpen = resolutionRepository.findByStatusAndOpenedAtBefore(ResolutionStatus.OPEN, threshold);
        for (ResolutionWindow window : expiredOpen) {
            closeWindowAuto(window);
        }
    }

    private void closeWindowAuto(ResolutionWindow window) {
        window.setStatus(ResolutionStatus.CLOSED);
        window.setClosedAt(LocalDateTime.now());
        resolutionRepository.save(window);
        Listing listing = listingService.getEntity(bookingService.getEntity(window.getBookingId()).getListingId());
        notificationService.notifyUser(listing.getOwnerId(), NotificationType.RESOLUTION_WINDOW_CLOSED_AUTO, Map.of("bookingId", window.getBookingId(), "resolutionId", window.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResolutionWindow getEntity(Long id) {
        return findWindow(id);
    }
}
