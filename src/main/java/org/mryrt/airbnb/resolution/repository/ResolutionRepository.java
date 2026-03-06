package org.mryrt.airbnb.resolution.repository;

import org.mryrt.airbnb.resolution.model.ResolutionWindow;
import org.mryrt.airbnb.resolution.repository.filter.ResolutionFilter;
import org.mryrt.airbnb.resolution.repository.specifications.ResolutionSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.mryrt.airbnb.resolution.model.ResolutionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ResolutionRepository extends JpaRepository<ResolutionWindow, Long>, JpaSpecificationExecutor<ResolutionWindow> {

    Optional<ResolutionWindow> findByBookingId(Long bookingId);

    List<ResolutionWindow> findByStatusAndOpenedAtBefore(ResolutionStatus status, LocalDateTime openedAtBefore);

    default Page<ResolutionWindow> findWithFilter(ResolutionFilter filter, Pageable pageable, List<Long> scopeBookingIds) {
        Specification<ResolutionWindow> spec = ResolutionSpecifications.withFilter(filter, scopeBookingIds);
        return findAll(spec, pageable);
    }
}
