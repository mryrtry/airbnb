package org.mryrt.airbnb.listing.access;

import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.auth.model.Permission;
import org.mryrt.airbnb.auth.service.user.UserService;
import org.mryrt.airbnb.exception.ServiceException;
import org.mryrt.airbnb.listing.model.Listing;
import org.mryrt.airbnb.listing.repository.ListingRepository;
import org.springframework.stereotype.Service;

import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.CANNOT_ACCESS_SOURCE;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.SOURCE_WITH_ID_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ListingAccessServiceImpl implements ListingAccessService {

    private final ListingRepository listingRepository;
    private final UserService userService;

    @Override
    public void requireCanModify(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "Listing", listingId));
        Long me = userService.getAuthenticatedUser().getId();
        if (listing.getOwnerId().equals(me)) return;
        if (userService.authenticatedUserHasPermission(Permission.ALL_LISTING_UPDATE)
                || userService.authenticatedUserHasPermission(Permission.ALL_LISTING_DELETE)) {
            return;
        }
        throw new ServiceException(CANNOT_ACCESS_SOURCE, "Listing", listingId);
    }
}
