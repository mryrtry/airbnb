package org.mryrt.airbnb.booking.access;

import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.auth.model.Permission;
import org.mryrt.airbnb.auth.service.user.UserService;
import org.mryrt.airbnb.booking.model.Booking;
import org.mryrt.airbnb.booking.repository.BookingRepository;
import org.mryrt.airbnb.exception.ServiceException;
import org.mryrt.airbnb.listing.model.Listing;
import org.mryrt.airbnb.listing.service.ListingService;
import org.springframework.stereotype.Service;

import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.CANNOT_ACCESS_SOURCE;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.SOURCE_WITH_ID_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class BookingAccessServiceImpl implements BookingAccessService {

    private final BookingRepository bookingRepository;
    private final ListingService listingService;
    private final UserService userService;

    @Override
    public void requireCanRead(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "Booking", bookingId));
        if (canRead(booking)) return;
        throw new ServiceException(CANNOT_ACCESS_SOURCE, "Booking", bookingId);
    }

    @Override
    public void requireCanUpdate(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "Booking", bookingId));
        if (canUpdate(booking)) return;
        throw new ServiceException(CANNOT_ACCESS_SOURCE, "Booking", bookingId);
    }

    @Override
    public void requireCanCheckInCheckOut(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "Booking", bookingId));
        Long me = userService.getAuthenticatedUser().getId();
        if (booking.getGuestId().equals(me)) return;
        if (userService.authenticatedUserHasPermission(Permission.ALL_BOOKING_UPDATE)) return;
        throw new ServiceException(CANNOT_ACCESS_SOURCE, "Booking", bookingId);
    }

    @Override
    public void requireCanDecide(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "Booking", bookingId));
        Listing listing = listingService.getEntity(booking.getListingId());
        Long me = userService.getAuthenticatedUser().getId();
        if (listing.getOwnerId().equals(me)) return;
        if (userService.authenticatedUserHasPermission(Permission.ALL_BOOKING_UPDATE)) return;
        throw new ServiceException(CANNOT_ACCESS_SOURCE, "Booking", bookingId);
    }

    @Override
    public Long getScopeUserIdForList() {
        if (userService.authenticatedUserHasPermission(Permission.ALL_BOOKING_READ)) {
            return null;
        }
        return userService.getAuthenticatedUser().getId();
    }

    private boolean canRead(Booking booking) {
        Long me = userService.getAuthenticatedUser().getId();
        if (booking.getGuestId().equals(me)) return true;
        Listing listing = listingService.getEntity(booking.getListingId());
        if (listing.getOwnerId().equals(me)) return true;
        return userService.authenticatedUserHasPermission(Permission.ALL_BOOKING_READ);
    }

    private boolean canUpdate(Booking booking) {
        Long me = userService.getAuthenticatedUser().getId();
        if (booking.getGuestId().equals(me)) return true;
        Listing listing = listingService.getEntity(booking.getListingId());
        if (listing.getOwnerId().equals(me)) return true;
        return userService.authenticatedUserHasPermission(Permission.ALL_BOOKING_UPDATE);
    }
}
