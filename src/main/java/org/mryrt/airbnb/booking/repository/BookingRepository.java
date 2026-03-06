package org.mryrt.airbnb.booking.repository;

import org.mryrt.airbnb.booking.model.Booking;
import org.mryrt.airbnb.booking.repository.filter.BookingFilter;
import org.mryrt.airbnb.booking.repository.specifications.BookingSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    default Page<Booking> findWithFilter(BookingFilter filter, Pageable pageable, Long scopeUserId) {
        Specification<Booking> spec = BookingSpecifications.withFilter(filter, scopeUserId);
        return findAll(spec, pageable);
    }

    /** Booking IDs where the user is guest or listing owner (for resolution scope). */
    @Query("SELECT b.id FROM Booking b, Listing l WHERE b.listingId = l.id AND (b.guestId = :userId OR l.ownerId = :userId)")
    List<Long> findBookingIdsByGuestOrOwner(@Param("userId") Long userId);
}
