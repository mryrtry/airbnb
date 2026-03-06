package org.mryrt.airbnb.util.pageable.sort;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DefaultSortConfigProvider implements SortConfigProvider {

    private final Map<Class<?>, SortConfig> sortConfigs;

    public DefaultSortConfigProvider(List<SortConfig> configs) {
        this.sortConfigs = configs.stream()
                .collect(Collectors.toMap(SortConfig::getEntityClass, config -> config));
    }

    @Override
    public SortConfig getSortConfig(Class<?> entityClass) {
        if (sortConfigs.containsKey(entityClass)) {
            return sortConfigs.get(entityClass);
        }
        throw new IllegalArgumentException("No sort configuration for: " + entityClass);
    }

}
