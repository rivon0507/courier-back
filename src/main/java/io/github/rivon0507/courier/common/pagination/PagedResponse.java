package io.github.rivon0507.courier.common.pagination;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(
        List<T> _items,
        PageInfo _page,
        SortInfo _sort
) {
    public static <T> PagedResponse<T> fromPage(@NonNull Page<T> page) {
        return new PagedResponse<>(
                page.toList(),
                new PageInfo(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()),
                new SortInfo(page.getSort().stream().findFirst().orElseThrow())
        );
    }
}
