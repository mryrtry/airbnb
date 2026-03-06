package org.mryrt.airbnb.listing.repository;

import org.mryrt.airbnb.listing.model.Listing;
import org.mryrt.airbnb.listing.repository.filter.ListingFilter;
import org.mryrt.airbnb.listing.repository.specifications.ListingSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    default Page<Listing> findWithFilter(ListingFilter filter, Pageable pageable) {
        Specification<Listing> spec = ListingSpecifications.withFilter(filter);
        return findAll(spec, pageable);
    }
}
