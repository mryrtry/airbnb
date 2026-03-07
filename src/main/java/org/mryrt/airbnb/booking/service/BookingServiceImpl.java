package org.mryrt.airbnb.booking.service;

import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.booking.access.BookingAccessService;
import org.mryrt.airbnb.booking.dto.BookingDto;
import org.mryrt.airbnb.booking.dto.BookingMapper;
import org.mryrt.airbnb.booking.dto.request.CreateBookingRequest;
import org.mryrt.airbnb.booking.dto.request.DecideBookingRequest;
import org.mryrt.airbnb.auth.service.user.UserService;
import org.mryrt.airbnb.booking.model.Booking;
import org.mryrt.airbnb.booking.model.BookingStatus;
import org.mryrt.airbnb.booking.repository.BookingRepository;
import org.mryrt.airbnb.booking.repository.filter.BookingFilter;
import org.mryrt.airbnb.event.BookingCheckedOutEvent;
import org.mryrt.airbnb.event.EntityEvent;
import org.mryrt.airbnb.exception.ServiceException;
import org.mryrt.airbnb.listing.model.Listing;
import org.mryrt.airbnb.listing.service.ListingService;
import org.mryrt.airbnb.notification.model.NotificationType;
import org.mryrt.airbnb.notification.service.NotificationService;
import org.mryrt.airbnb.util.pageable.PageableFactory;
import org.mryrt.airbnb.util.pageable.PageableRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mryrt.airbnb.event.EventType.CREATED;
import static org.mryrt.airbnb.event.EventType.UPDATED;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.BOOKING_CHECK_OUT_AFTER_CHECK_IN;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.BOOKING_GUEST_OVERLAPPING_DATES;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.BOOKING_LISTING_OCCUPIED_DATES;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.ID_MUST_BE_POSITIVE;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.MUST_BE_NOT_NULL;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.SOURCE_WITH_ID_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingMapper mapper;
    private final BookingAccessService bookingAccessService;
    private final ListingService listingService;
    private final UserService userService;
    private final PageableFactory pageableFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    private Booking findBooking(Long id) {
        if (id == null) throw new ServiceException(MUST_BE_NOT_NULL, "Booking.id");
        if (id <= 0) throw new ServiceException(ID_MUST_BE_POSITIVE, "Booking.id");
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "Booking", id));
    }

    @Override
    @Transactional
    public BookingDto create(CreateBookingRequest request) {
        if (request.getCheckOutDate() == null || !request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new ServiceException(BOOKING_CHECK_OUT_AFTER_CHECK_IN);
        }
        Listing listing = listingService.getEntity(request.getListingId());
        Long guestId = userService.getAuthenticatedUser().getId();
        if (bookingRepository.existsOverlappingByGuestAndListing(guestId, request.getListingId(),
                request.getCheckInDate(), request.getCheckOutDate(),
                List.of(BookingStatus.APPLIED, BookingStatus.APPROVED, BookingStatus.CHECKED_IN))) {
            throw new ServiceException(BOOKING_GUEST_OVERLAPPING_DATES);
        }
        Booking booking = Booking.builder()
                .listingId(request.getListingId())
                .guestId(guestId)
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .status(BookingStatus.APPLIED)
                .appliedAt(LocalDateTime.now())
                .build();
        BookingDto dto = mapper.toDto(bookingRepository.save(booking));
        eventPublisher.publishEvent(new EntityEvent<>(CREATED, dto));
        notificationService.notifyUser(listing.getOwnerId(), NotificationType.BOOKING_APPLIED,
                Map.of("listingId", listing.getId(), "bookingId", dto.getId()));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingDto> getAll(BookingFilter filter, PageableRequest pageable) {
        Pageable page = pageableFactory.create(pageable, Booking.class);
        Long scopeUserId = bookingAccessService.getScopeUserIdForList();
        return bookingRepository.findWithFilter(filter, page, scopeUserId).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDto get(Long id) {
        bookingAccessService.requireCanRead(id);
        return mapper.toDto(findBooking(id));
    }

    @Override
    @Transactional
    public BookingDto decide(Long id, DecideBookingRequest request) {
        if (request.getStatus() != BookingStatus.APPROVED && request.getStatus() != BookingStatus.REJECTED) {
            throw new ServiceException(MUST_BE_NOT_NULL, "Booking.status (APPROVED or REJECTED)");
        }
        bookingAccessService.requireCanDecide(id);
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.APPLIED) {
            throw new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "Booking (already decided)", id);
        }
        booking.setStatus(request.getStatus());
        if (request.getStatus() == BookingStatus.APPROVED && booking.getCheckInDate() != null && booking.getCheckOutDate() != null) {
            if (bookingRepository.existsOverlappingApprovedOrCheckedIn(booking.getListingId(),
                    booking.getCheckInDate(), booking.getCheckOutDate(), id)) {
                throw new ServiceException(BOOKING_LISTING_OCCUPIED_DATES);
            }
            List<Booking> overlapping = bookingRepository.findOverlappingAppliedByListingExcluding(
                    booking.getListingId(), booking.getCheckInDate(), booking.getCheckOutDate(), id);
            for (Booking other : overlapping) {
                other.setStatus(BookingStatus.REJECTED);
                bookingRepository.save(other);
                notificationService.notifyUser(other.getGuestId(), NotificationType.BOOKING_REJECTED_ANOTHER_APPROVED,
                        Map.of("bookingId", other.getId(), "listingId", other.getListingId()));
            }
        }
        BookingDto dto = mapper.toDto(bookingRepository.save(booking));
        eventPublisher.publishEvent(new EntityEvent<>(UPDATED, dto));
        if (request.getStatus() == BookingStatus.APPROVED) {
            notificationService.notifyUser(booking.getGuestId(), NotificationType.BOOKING_APPROVED, Map.of("bookingId", id));
        } else {
            notificationService.notifyUser(booking.getGuestId(), NotificationType.BOOKING_REJECTED, Map.of("bookingId", id));
        }
        return dto;
    }

    @Override
    @Transactional
    public BookingDto recordCheckIn(Long id) {
        bookingAccessService.requireCanCheckInCheckOut(id);
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "Booking (only APPROVED)", id);
        }
        Listing listing = listingService.getEntity(booking.getListingId());
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckInAt(LocalDateTime.now());
        booking = bookingRepository.save(booking);
        notificationService.notifyUser(listing.getOwnerId(), NotificationType.BOOKING_CHECKED_IN, Map.of("bookingId", id));
        return mapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingDto recordCheckOut(Long id) {
        bookingAccessService.requireCanCheckInCheckOut(id);
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "Booking (only CHECKED_IN)", id);
        }
        Listing listing = listingService.getEntity(booking.getListingId());
        booking.setStatus(BookingStatus.CHECKED_OUT);
        booking.setCheckOutAt(LocalDateTime.now());
        BookingDto dto = mapper.toDto(bookingRepository.save(booking));
        eventPublisher.publishEvent(new EntityEvent<>(UPDATED, dto));
        notificationService.notifyUser(listing.getOwnerId(), NotificationType.BOOKING_CHECKED_OUT, Map.of("bookingId", id));
        eventPublisher.publishEvent(new BookingCheckedOutEvent(booking.getId()));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getEntity(Long id) {
        return findBooking(id);
    }
}
