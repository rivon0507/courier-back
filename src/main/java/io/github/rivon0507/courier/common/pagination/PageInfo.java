package io.github.rivon0507.courier.common.pagination;

public record PageInfo(
        int pageIndex,
        int pageSize,
        long totalElements,
        long totalPages
) {
}
