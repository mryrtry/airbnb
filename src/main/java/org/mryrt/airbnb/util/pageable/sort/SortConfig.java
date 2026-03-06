package org.mryrt.airbnb.util.pageable.sort;

import java.util.Set;

public interface SortConfig {

    Class<?> getEntityClass();

    Set<String> getAllowedSortFields();

    String getDefaultSortField();

}
