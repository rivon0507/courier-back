package io.github.rivon0507.courier.common.pagination;

import org.springframework.data.domain.Sort;

public record SortInfo(
        String key,
        Direction direction
) {
    public SortInfo(Sort.Order order) {
        this(order.getProperty(), order.getDirection().isAscending() ? Direction.ASC : Direction.DESC);
    }

    public enum Direction {ASC, DESC}
}
