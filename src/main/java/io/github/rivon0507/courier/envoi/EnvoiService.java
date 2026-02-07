package io.github.rivon0507.courier.envoi;

import io.github.rivon0507.courier.common.pagination.PagedResponse;
import io.github.rivon0507.courier.envoi.api.EnvoiCreateRequest;
import io.github.rivon0507.courier.envoi.api.EnvoiResponse;
import io.github.rivon0507.courier.envoi.api.EnvoiUpdateRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class EnvoiService {
    public EnvoiResponse create(EnvoiCreateRequest request, Long workspaceId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public PagedResponse<EnvoiResponse> getPage(Pageable page, Long workspaceId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public EnvoiResponse update(String envoiId, Long workspaceId, EnvoiUpdateRequest requestBody) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void delete(String envoiId, Long workspaceId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public EnvoiResponse get(Long envoiId, Long workspaceId) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
