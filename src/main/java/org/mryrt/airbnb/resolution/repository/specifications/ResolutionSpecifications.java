package org.mryrt.airbnb.resolution.repository.specifications;

import org.mryrt.airbnb.resolution.model.ResolutionWindow;
import org.mryrt.airbnb.resolution.repository.filter.ResolutionFilter;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class ResolutionSpecifications {

    public static Specification<ResolutionWindow> withFilter(ResolutionFilter filter) {
        return withFilter(filter, null);
    }

    /**
     * @param scopeBookingIds if non-null and non-empty, restrict to resolutions for these booking IDs (for non-admin).
     */
    public static Specification<ResolutionWindow> withFilter(ResolutionFilter filter, List<Long> scopeBookingIds) {
        Specification<ResolutionWindow> base = Specification.<ResolutionWindow>where(
                        filter == null || filter.getBookingId() == null ? null : (root, q, cb) -> cb.equal(root.get("bookingId"), filter.getBookingId()))
                .and(filter == null || filter.getStatus() == null ? null : (root, q, cb) -> cb.equal(root.get("status"), filter.getStatus()))
                .and(filter == null || filter.getOpenedAtAfter() == null ? null : (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("openedAt"), filter.getOpenedAtAfter()))
                .and(filter == null || filter.getOpenedAtBefore() == null ? null : (root, q, cb) -> cb.lessThanOrEqualTo(root.get("openedAt"), filter.getOpenedAtBefore()));
        if (scopeBookingIds == null || scopeBookingIds.isEmpty()) {
            return base;
        }
        return base.and((root, q, cb) -> root.get("bookingId").in(scopeBookingIds));
    }
}
