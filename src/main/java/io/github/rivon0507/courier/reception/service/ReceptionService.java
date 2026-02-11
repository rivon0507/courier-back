package io.github.rivon0507.courier.reception.service;

import io.github.rivon0507.courier.common.api.PieceCreateRequest;
import io.github.rivon0507.courier.common.api.PieceResponse;
import io.github.rivon0507.courier.common.api.PieceUpdateRequest;
import io.github.rivon0507.courier.common.domain.Workspace;
import io.github.rivon0507.courier.common.pagination.PagedResponse;
import io.github.rivon0507.courier.common.persistence.WorkspaceRepository;
import io.github.rivon0507.courier.common.web.error.DuplicatePieceIdException;
import io.github.rivon0507.courier.common.web.error.WorkspaceNotFoundException;
import io.github.rivon0507.courier.reception.api.ReceptionCreateRequest;
import io.github.rivon0507.courier.reception.api.ReceptionDetailsResponse;
import io.github.rivon0507.courier.reception.api.ReceptionResponse;
import io.github.rivon0507.courier.reception.api.ReceptionUpdateRequest;
import io.github.rivon0507.courier.reception.domain.Reception;
import io.github.rivon0507.courier.reception.domain.ReceptionPiece;
import io.github.rivon0507.courier.reception.persistence.ReceptionPieceRepository;
import io.github.rivon0507.courier.reception.persistence.ReceptionRepository;
import io.github.rivon0507.courier.reception.web.error.ReceptionNotFoundException;
import io.github.rivon0507.courier.reception.web.error.ReceptionPieceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReceptionService {

    private final WorkspaceRepository workspaceRepository;
    private final ReceptionRepository receptionRepository;
    private final ReceptionPieceMapper pieceMapper;
    private final ReceptionMapper receptionMapper;
    private final ReceptionPieceMapper receptionPieceMapper;
    private final ReceptionPieceRepository receptionPieceRepository;

    public ReceptionDetailsResponse create(ReceptionCreateRequest request, Long workspaceId, Long userId) {
        Workspace workspace = findOwnedWorkspaceOrThrow(workspaceId, userId);
        Reception reception = receptionMapper.fromCreateRequest(request);
        reception.setWorkspace(workspace);
        if (request.pieces() != null) {
            List<ReceptionPiece> pieces = request.pieces().stream()
                    .map(pieceMapper::fromPieceCreateRequest)
                    .peek(p -> p.setReception(reception))
                    .toList();
            reception.setPieces(pieces);
        }
        receptionRepository.save(reception);
        return receptionMapper.toDetailsResponse(reception);
    }

    public ReceptionResponse get(Long receptionId, Long workspaceId, Long userId) {
        ensureWorkspaceBelongsToUser(workspaceId, userId);
        Reception reception = receptionRepository.findByIdAndWorkspace_Id(receptionId, workspaceId)
                .orElseThrow(ReceptionNotFoundException::new);
        return receptionMapper.toResponse(reception);
    }

    public PagedResponse<ReceptionResponse> getPage(Pageable page, Long workspaceId, Long userId) {
        ensureWorkspaceBelongsToUser(workspaceId, userId);
        var receptionResponsePage = receptionRepository.findAllByWorkspace_Id(workspaceId, page)
                .map(receptionMapper::toResponse);
        return PagedResponse.fromPage(receptionResponsePage);
    }

    public ReceptionResponse update(Long receptionId, ReceptionUpdateRequest requestBody, Long workspaceId, Long userId) {
        ensureWorkspaceBelongsToUser(workspaceId, userId);
        Reception reception = receptionRepository.findByIdAndWorkspace_Id(receptionId, workspaceId)
                .orElseThrow(ReceptionNotFoundException::new);
        receptionMapper.updateFromRequest(reception, requestBody);
        receptionRepository.save(reception);
        return receptionMapper.toResponse(reception);
    }

    @Transactional
    public void delete(Long receptionId, Long workspaceId, Long userId) {
        ensureWorkspaceBelongsToUser(workspaceId, userId);
        receptionRepository.deleteByIdAndWorkspace_Id(receptionId, workspaceId);
    }

    @Transactional
    public List<PieceResponse> createPieces(Long receptionId, List<PieceCreateRequest> request, Long workspaceId, Long userId) {
        ensureWorkspaceBelongsToUser(workspaceId, userId);
        Reception reception = receptionRepository.findByIdAndWorkspace_Id(receptionId, workspaceId)
                .orElseThrow(ReceptionNotFoundException::new);
        List<ReceptionPiece> pieceList = request.stream()
                .map(dto -> {
                    ReceptionPiece piece = pieceMapper.fromPieceCreateRequest(dto);
                    reception.getPieces().add(piece);
                    piece.setReception(reception);
                    return piece;
                })
                .collect(Collectors.toList());
        List<ReceptionPiece> savedPieces = receptionPieceRepository.saveAllAndFlush(pieceList);
        return receptionPieceMapper.toResponseList(savedPieces);
    }

    public PagedResponse<PieceResponse> getPiecesPage(Long receptionId, Pageable page, Long workspaceId, Long userId) {
        ensureWorkspaceBelongsToUser(workspaceId, userId);
        Reception reception = receptionRepository.findByIdAndWorkspace_Id(receptionId, workspaceId)
                .orElseThrow(ReceptionNotFoundException::new);
        Page<PieceResponse> pieces = receptionPieceRepository.findAllByReception(reception, page).map(receptionPieceMapper::toResponse);
        return PagedResponse.fromPage(pieces);
    }

    @Transactional
    public List<PieceResponse> updatePieces(Long receptionId, List<PieceUpdateRequest> request, Long workspaceId, Long userId) {
        ensureWorkspaceBelongsToUser(workspaceId, userId);
        Reception reception = receptionRepository.findByIdAndWorkspace_Id(receptionId, workspaceId)
                .orElseThrow(ReceptionNotFoundException::new);
        List<Long> ids = request.stream().map(PieceUpdateRequest::id).toList();
        List<ReceptionPiece> receptionPieces = receptionPieceRepository.findAllByReceptionAndIdIn(reception, ids);
        Map<Long, ReceptionPiece> pieceMap = receptionPieces.stream().collect(Collectors.toMap(ReceptionPiece::getId, piece -> piece));

        Set<Long> seenIds = new HashSet<>(pieceMap.size());
        return request.stream()
                .map(r -> {
                    ReceptionPiece piece = pieceMap.get(r.id());
                    if (piece == null) throw new ReceptionPieceNotFoundException();
                    if (!seenIds.add(r.id())) throw new DuplicatePieceIdException();
                    receptionPieceMapper.updateFromRequest(piece, r);
                    return receptionPieceMapper.toResponse(piece);
                }).toList();
    }

    @Transactional
    public void deletePieces(Long receptionId, List<Long> pieceIds, Long workspaceId, Long userId) {
        ensureWorkspaceBelongsToUser(workspaceId, userId);
        Reception reception = receptionRepository.findByIdAndWorkspace_Id(receptionId, workspaceId)
                .orElseThrow(ReceptionNotFoundException::new);
        receptionPieceRepository.deleteAllByReceptionAndIdIn(reception, pieceIds);
    }

    private @NonNull Workspace findOwnedWorkspaceOrThrow(Long workspaceId, Long userId) {
        return workspaceRepository.findById(workspaceId)
                .filter(w -> Objects.equals(w.getOwner().getId(), userId))
                .orElseThrow(WorkspaceNotFoundException::new);
    }

    private void ensureWorkspaceBelongsToUser(Long workspaceId, Long userId) {
        if (!workspaceRepository.existsByIdAndOwner_Id(workspaceId, userId)) throw new WorkspaceNotFoundException();
    }
}
