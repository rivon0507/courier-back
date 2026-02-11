package io.github.rivon0507.courier.reception;

import io.github.rivon0507.courier.common.api.PieceCreateRequest;
import io.github.rivon0507.courier.common.api.PieceResponse;
import io.github.rivon0507.courier.common.api.PieceUpdateRequest;
import io.github.rivon0507.courier.common.pagination.PagedResponse;
import io.github.rivon0507.courier.reception.api.ReceptionCreateRequest;
import io.github.rivon0507.courier.reception.api.ReceptionDetailsResponse;
import io.github.rivon0507.courier.reception.api.ReceptionResponse;
import io.github.rivon0507.courier.reception.api.ReceptionUpdateRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class ReceptionService {

    public ReceptionDetailsResponse create(ReceptionCreateRequest request, Long workspaceId, Long userId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public ReceptionResponse get(Long receptionId, Long workspaceId, Long userId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public PagedResponse<ReceptionResponse> getPage(Pageable page, Long workspaceId, Long userId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public ReceptionResponse update(Long receptionId, ReceptionUpdateRequest requestBody, Long workspaceId, Long userId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void delete(Long receptionId, Long workspaceId, Long userId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public List<PieceResponse> createPieces(Long receptionId, List<PieceCreateRequest> request, Long workspaceId, Long userId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public PagedResponse<PieceResponse> getPiecesPage(Long receptionId, Pageable page, Long workspaceId, Long userId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public List<PieceResponse> updatePieces(Long receptionId, List<PieceUpdateRequest> request, Long workspaceId, Long userId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void deletePieces(Long receptionId, List<Long> ids, Long workspaceId, Long userId) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
