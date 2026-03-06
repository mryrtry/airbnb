package org.mryrt.airbnb.auth.repository.specifications;

import org.mryrt.airbnb.auth.model.User;
import org.mryrt.airbnb.auth.repository.filter.UserFilter;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    public static Specification<User> withFilter(UserFilter filter) {
        if (filter == null) return null;

        return Specification.<User>where((root, query, cb) ->
                        filter.getUsername() != null ? cb.like(root.get("username"), "%" + filter.getUsername() + "%") : null)
                .and((root, query, cb) ->
                        filter.getCreatedAtAfter() != null ? cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedAtAfter()) : null)
                .and((root, query, cb) ->
                        filter.getCreatedAtBefore() != null ? cb.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedAtBefore()) : null)
                .and((root, query, cb) ->
                        filter.getUpdatedAtAfter() != null ? cb.greaterThanOrEqualTo(root.get("updatedAt"), filter.getUpdatedAtAfter()) : null)
                .and((root, query, cb) ->
                        filter.getUpdatedAtBefore() != null ? cb.lessThanOrEqualTo(root.get("updatedAt"), filter.getUpdatedAtBefore()) : null);
    }

}