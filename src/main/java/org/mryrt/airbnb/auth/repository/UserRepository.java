package org.mryrt.airbnb.auth.repository;

import org.mryrt.airbnb.auth.model.User;
import org.mryrt.airbnb.auth.repository.filter.UserFilter;
import org.mryrt.airbnb.auth.repository.specifications.UserSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    default Page<User> findWithFilter(UserFilter filter, Pageable pageable) {
        Specification<User> specification = UserSpecifications.withFilter(filter);
        return findAll(specification, pageable);
    }

}