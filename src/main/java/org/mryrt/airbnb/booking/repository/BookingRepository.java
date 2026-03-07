package org.mryrt.airbnb.booking.repository;

import org.mryrt.airbnb.booking.model.Booking;
import org.mryrt.airbnb.booking.model.BookingStatus;
import org.mryrt.airbnb.booking.repository.filter.BookingFilter;
import org.mryrt.airbnb.booking.repository.specifications.BookingSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    /**
     * Есть ли у гостя по этому листингу брони со статусом APPLIED/APPROVED/CHECKED_IN и пересекающимися датами.
     * Пересечение: [checkIn, checkOut] — в дату выезда может заехать другой, поэтому считаем checkOut исключительно:
     * overlap = checkIn1 < checkOut2 && checkOut1 > checkIn2.
     */
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.guestId = :guestId AND b.listingId = :listingId AND b.status IN :statuses " +
            "AND b.checkInDate IS NOT NULL AND b.checkOutDate IS NOT NULL " +
            "AND b.checkInDate < :checkOutDate AND b.checkOutDate > :checkInDate")
    boolean existsOverlappingByGuestAndListing(@Param("guestId") Long guestId, @Param("listingId") Long listingId,
                                                @Param("checkInDate") LocalDate checkInDate, @Param("checkOutDate") LocalDate checkOutDate,
                                                @Param("statuses") List<BookingStatus> statuses);

    /**
     * Все брони по листингу со статусом APPLIED с пересекающимися датами, кроме указанного id.
     */
    @Query("SELECT b FROM Booking b WHERE b.listingId = :listingId AND b.status = org.mryrt.airbnb.booking.model.BookingStatus.APPLIED " +
            "AND b.id <> :excludeId AND b.checkInDate IS NOT NULL AND b.checkOutDate IS NOT NULL " +
            "AND b.checkInDate < :checkOutDate AND b.checkOutDate > :checkInDate")
    List<Booking> findOverlappingAppliedByListingExcluding(@Param("listingId") Long listingId,
                                                           @Param("checkInDate") LocalDate checkInDate, @Param("checkOutDate") LocalDate checkOutDate,
                                                           @Param("excludeId") Long excludeId);

    /** Есть ли уже одобренная/занятая бронь по листингу на эти даты (кроме excludeId). */
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.listingId = :listingId AND b.id <> :excludeId " +
            "AND b.status IN (org.mryrt.airbnb.booking.model.BookingStatus.APPROVED, org.mryrt.airbnb.booking.model.BookingStatus.CHECKED_IN) " +
            "AND b.checkInDate IS NOT NULL AND b.checkOutDate IS NOT NULL " +
            "AND b.checkInDate < :checkOutDate AND b.checkOutDate > :checkInDate")
    boolean existsOverlappingApprovedOrCheckedIn(@Param("listingId") Long listingId,
                                                 @Param("checkInDate") LocalDate checkInDate, @Param("checkOutDate") LocalDate checkOutDate,
                                                 @Param("excludeId") Long excludeId);

    default Page<Booking> findWithFilter(BookingFilter filter, Pageable pageable, Long scopeUserId) {
        Specification<Booking> spec = BookingSpecifications.withFilter(filter, scopeUserId);
        return findAll(spec, pageable);
    }

    /** Booking IDs where the user is guest or listing owner (for resolution scope). */
    @Query("SELECT b.id FROM Booking b, Listing l WHERE b.listingId = l.id AND (b.guestId = :userId OR l.ownerId = :userId)")
    List<Long> findBookingIdsByGuestOrOwner(@Param("userId") Long userId);
}
