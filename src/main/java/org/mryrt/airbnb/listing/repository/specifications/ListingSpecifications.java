package org.mryrt.airbnb.listing.repository.specifications;

import org.mryrt.airbnb.listing.model.Listing;
import org.mryrt.airbnb.listing.repository.filter.ListingFilter;
import org.springframework.data.jpa.domain.Specification;

public final class ListingSpecifications {

    public static Specification<Listing> withFilter(ListingFilter filter) {
        if (filter == null) return null;

        return Specification.<Listing>where(
                        filter.getOwnerId() == null ? null : (root, query, cb) -> cb.equal(root.get("ownerId"), filter.getOwnerId()))
                .and(filter.getStatus() == null ? null : (root, query, cb) -> cb.equal(root.get("status"), filter.getStatus()))
                .and(filter.getTitleLike() == null || filter.getTitleLike().isBlank() ? null
                        : (root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + filter.getTitleLike().toLowerCase() + "%"));
    }
}
