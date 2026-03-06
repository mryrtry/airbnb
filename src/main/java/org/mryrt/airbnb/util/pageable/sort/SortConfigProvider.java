package org.mryrt.airbnb.util.pageable.sort;

public interface SortConfigProvider {

    SortConfig getSortConfig(Class<?> entityClass);

}
