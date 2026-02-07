package io.github.rivon0507.courier.common.pagination;

public record SortInfo(
        String key,
        Direction direction
) {
    public enum Direction {ASC, DESC}
}
