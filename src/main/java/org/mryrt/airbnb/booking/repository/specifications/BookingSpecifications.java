package org.mryrt.airbnb.booking.repository.specifications;

import org.mryrt.airbnb.booking.model.Booking;
import org.mryrt.airbnb.booking.repository.filter.BookingFilter;
import org.mryrt.airbnb.listing.model.Listing;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Subquery;

public final class BookingSpecifications {

    public static Specification<Booking> withFilter(BookingFilter filter) {
        return withFilter(filter, null);
    }

    /**
     * @param scopeUserId if non-null, restrict to bookings where user is guest or listing owner (for non-admin).
     */
    public static Specification<Booking> withFilter(BookingFilter filter, Long scopeUserId) {
        Specification<Booking> base = Specification.<Booking>where(
                        filter == null || filter.getListingId() == null ? null : (root, q, cb) -> cb.equal(root.get("listingId"), filter.getListingId()))
                .and(filter == null || filter.getGuestId() == null ? null : (root, q, cb) -> cb.equal(root.get("guestId"), filter.getGuestId()))
                .and(filter == null || filter.getStatus() == null ? null : (root, q, cb) -> cb.equal(root.get("status"), filter.getStatus()))
                .and(filter == null || filter.getCheckInDateFrom() == null ? null : (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("checkInDate"), filter.getCheckInDateFrom()))
                .and(filter == null || filter.getCheckOutDateTo() == null ? null : (root, q, cb) -> cb.lessThanOrEqualTo(root.get("checkOutDate"), filter.getCheckOutDateTo()))
                .and(filter == null || filter.getAppliedAtAfter() == null ? null : (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("appliedAt"), filter.getAppliedAtAfter()))
                .and(filter == null || filter.getAppliedAtBefore() == null ? null : (root, q, cb) -> cb.lessThanOrEqualTo(root.get("appliedAt"), filter.getAppliedAtBefore()));
        if (scopeUserId == null) {
            return base;
        }
        return base.and(scopeForUser(scopeUserId));
    }

    /** Bookings where user is guest or owner of the listing. */
    public static Specification<Booking> scopeForUser(Long userId) {
        return (root, query, cb) -> {
            Subquery<Long> ownerListingIds = query.subquery(Long.class);
            var listingRoot = ownerListingIds.from(Listing.class);
            ownerListingIds.select(listingRoot.get("id"));
            ownerListingIds.where(cb.equal(listingRoot.get("ownerId"), userId));
            return cb.or(
                    cb.equal(root.get("guestId"), userId),
                    root.get("listingId").in(ownerListingIds)
            );
        };
    }
}
