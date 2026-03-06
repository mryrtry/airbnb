package org.mryrt.airbnb.util.pageable;

import org.springframework.data.domain.Pageable;

public interface PageableFactory {

    Pageable create(PageableRequest request, Class<?> entityClass);

}
