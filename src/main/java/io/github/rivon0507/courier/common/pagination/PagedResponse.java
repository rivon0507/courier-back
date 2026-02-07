package io.github.rivon0507.courier.common.pagination;

import java.util.List;

public record PagedResponse<T>(
        List<T> _items,
        PageInfo _page,
        SortInfo _sort
) {
}
