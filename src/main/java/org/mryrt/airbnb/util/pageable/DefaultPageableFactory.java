package org.mryrt.airbnb.util.pageable;

import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.util.pageable.constants.PageableConstants;
import org.mryrt.airbnb.util.pageable.sort.SortConfig;
import org.mryrt.airbnb.util.pageable.sort.SortConfigProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class DefaultPageableFactory implements PageableFactory {

    private final SortConfigProvider configProvider;

    private static Sort.Direction parseDirection(String direction) {
        try {
            return Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException e) {
            return Sort.Direction.ASC;
        }
    }

    @Override
    public Pageable create(PageableRequest config, Class<?> entityType) {
        SortConfig sortConfig = configProvider.getSortConfig(entityType);

        int page = Math.max(config.getPage(), 0);
        int size = Math.max(config.getSize(), PageableConstants.MIN_PAGE_SIZE);

        List<String> sortFields = (config.getSort() == null || config.getSort().isEmpty())
                ? List.of(sortConfig.getDefaultSortField())
                : config.getSort().stream()
                .filter(sortConfig.getAllowedSortFields()::contains)
                .toList();

        if (sortFields.isEmpty()) {
            sortFields = List.of(sortConfig.getDefaultSortField());
        }

        Sort sort = Sort.by(parseDirection(config.getDirection()), sortFields.toArray(String[]::new));

        return PageRequest.of(page, size, sort);
    }

}
