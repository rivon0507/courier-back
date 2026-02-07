package io.github.rivon0507.courier.envoi;

import io.github.rivon0507.courier.common.pagination.PagedResponse;
import io.github.rivon0507.courier.envoi.api.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnvoiService {
    public EnvoiResponse create(EnvoiCreateRequest request, Long workspaceId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public PagedResponse<EnvoiResponse> getPage(Pageable page, Long workspaceId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public EnvoiResponse update(String envoiId, EnvoiUpdateRequest requestBody, Long workspaceId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void delete(String envoiId, Long workspaceId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public EnvoiResponse get(Long envoiId, Long workspaceId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public List<PieceResponse> createPieces(String envoiId, List<PieceCreateRequest> request, Long workspaceId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public List<PieceResponse> updatePieces(String envoiId, List<PieceUpdateRequest> request, Long workspaceId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void deletePieces(String envoiId, List<Long> request, Long workspaceId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public PagedResponse<PieceResponse> getPiecesPage(String envoiId, Pageable page, Long workspaceId) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
