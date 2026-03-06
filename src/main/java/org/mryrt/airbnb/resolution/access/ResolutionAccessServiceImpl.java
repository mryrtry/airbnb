package org.mryrt.airbnb.resolution.access;

import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.auth.model.Permission;
import org.mryrt.airbnb.auth.service.user.UserService;
import org.mryrt.airbnb.booking.model.Booking;
import org.mryrt.airbnb.booking.repository.BookingRepository;
import org.mryrt.airbnb.exception.ServiceException;
import org.mryrt.airbnb.listing.model.Listing;
import org.mryrt.airbnb.listing.service.ListingService;
import org.mryrt.airbnb.resolution.model.ResolutionWindow;
import org.mryrt.airbnb.resolution.repository.ResolutionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.CANNOT_ACCESS_SOURCE;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.SOURCE_WITH_ID_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ResolutionAccessServiceImpl implements ResolutionAccessService {

    private final ResolutionRepository resolutionRepository;
    private final BookingRepository bookingRepository;
    private final ListingService listingService;
    private final UserService userService;

    @Override
    public void requireCanRead(Long resolutionId) {
        ResolutionWindow window = resolutionRepository.findById(resolutionId)
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "ResolutionWindow", resolutionId));
        Booking booking = bookingRepository.findById(window.getBookingId())
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "Booking", window.getBookingId()));
        Listing listing = listingService.getEntity(booking.getListingId());
        Long me = userService.getAuthenticatedUser().getId();
        if (booking.getGuestId().equals(me) || listing.getOwnerId().equals(me)) return;
        if (userService.authenticatedUserHasPermission(Permission.ALL_RESOLUTION_READ)) return;
        throw new ServiceException(CANNOT_ACCESS_SOURCE, "ResolutionWindow", resolutionId);
    }

    @Override
    public void requireIsOwner(Long resolutionId) {
        ResolutionWindow window = resolutionRepository.findById(resolutionId)
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "ResolutionWindow", resolutionId));
        Booking booking = bookingRepository.findById(window.getBookingId())
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "Booking", window.getBookingId()));
        Listing listing = listingService.getEntity(booking.getListingId());
        Long me = userService.getAuthenticatedUser().getId();
        if (listing.getOwnerId().equals(me)) return;
        if (userService.authenticatedUserHasPermission(Permission.ALL_RESOLUTION_UPDATE)) return;
        throw new ServiceException(CANNOT_ACCESS_SOURCE, "ResolutionWindow", resolutionId);
    }

    @Override
    public void requireIsGuest(Long resolutionId) {
        ResolutionWindow window = resolutionRepository.findById(resolutionId)
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "ResolutionWindow", resolutionId));
        Booking booking = bookingRepository.findById(window.getBookingId())
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "Booking", window.getBookingId()));
        Long me = userService.getAuthenticatedUser().getId();
        if (booking.getGuestId().equals(me)) return;
        if (userService.authenticatedUserHasPermission(Permission.ALL_RESOLUTION_UPDATE)) return;
        throw new ServiceException(CANNOT_ACCESS_SOURCE, "ResolutionWindow", resolutionId);
    }

    @Override
    public void requireCanClose(Long resolutionId) {
        if (userService.authenticatedUserHasPermission(Permission.ALL_RESOLUTION_UPDATE)) return;
        ResolutionWindow window = resolutionRepository.findById(resolutionId)
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "ResolutionWindow", resolutionId));
        if (window.getStatus() == org.mryrt.airbnb.resolution.model.ResolutionStatus.REFUSED) {
            Booking booking = bookingRepository.findById(window.getBookingId())
                    .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "Booking", window.getBookingId()));
            Listing listing = listingService.getEntity(booking.getListingId());
            if (listing.getOwnerId().equals(userService.getAuthenticatedUser().getId())) return;
        }
        throw new ServiceException(CANNOT_ACCESS_SOURCE, "ResolutionWindow", resolutionId);
    }

    @Override
    public void requireCanResolveEscalation(Long resolutionId) {
        if (userService.authenticatedUserHasPermission(Permission.ALL_RESOLUTION_UPDATE)) return;
        throw new ServiceException(CANNOT_ACCESS_SOURCE, "ResolutionWindow", resolutionId);
    }

    @Override
    public List<Long> getScopeBookingIdsForList() {
        if (userService.authenticatedUserHasPermission(Permission.ALL_RESOLUTION_READ)) {
            return null;
        }
        return bookingRepository.findBookingIdsByGuestOrOwner(userService.getAuthenticatedUser().getId());
    }
}
