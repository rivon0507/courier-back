package io.github.rivon0507.courier.envoi;

import io.github.rivon0507.courier.common.api.PieceCreateRequest;
import io.github.rivon0507.courier.common.api.PieceResponse;
import io.github.rivon0507.courier.common.api.PieceUpdateRequest;
import io.github.rivon0507.courier.common.domain.Workspace;
import io.github.rivon0507.courier.common.pagination.PagedResponse;
import io.github.rivon0507.courier.common.persistence.WorkspaceRepository;
import io.github.rivon0507.courier.envoi.api.EnvoiCreateRequest;
import io.github.rivon0507.courier.envoi.api.EnvoiDetailsResponse;
import io.github.rivon0507.courier.envoi.api.EnvoiResponse;
import io.github.rivon0507.courier.envoi.api.EnvoiUpdateRequest;
import io.github.rivon0507.courier.envoi.domain.Envoi;
import io.github.rivon0507.courier.envoi.domain.EnvoiPiece;
import io.github.rivon0507.courier.envoi.persistence.EnvoiPieceRepository;
import io.github.rivon0507.courier.envoi.persistence.EnvoiRepository;
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
public class EnvoiService {
    private final EnvoiMapper envoiMapper;
    private final EnvoiPieceMapper pieceMapper;
    private final EnvoiRepository envoiRepository;
    private final WorkspaceRepository workspaceRepository;
    private final EnvoiPieceMapper envoiPieceMapper;
    private final EnvoiPieceRepository envoiPieceRepository;

    public EnvoiDetailsResponse create(EnvoiCreateRequest request, Long workspaceId, Long userId) {
        Workspace workspace = findOwnedWorkspaceOrThrow(workspaceId, userId);
        Envoi envoi = envoiMapper.fromCreateRequest(request);
        envoi.setWorkspace(workspace);
        if (request.pieces() != null) {
            List<EnvoiPiece> pieces = request.pieces().stream()
                    .map(pieceMapper::fromPieceCreateRequest)
                    .peek(p -> p.setEnvoi(envoi))
                    .toList();
            envoi.setPieces(pieces);
        }
        envoiRepository.save(envoi);
        envoi.setReference("ENVOI-%d".formatted(envoi.getId()));
        envoiRepository.save(envoi);
        return envoiMapper.toDetailsResponse(envoi);
    }

    public PagedResponse<EnvoiResponse> getPage(Pageable page, Long workspaceId, Long userId) {
        ensureWorkspaceBelongsToUser(workspaceId, userId);
        var envoiResponsePage = envoiRepository.findAllByWorkspace_Id(workspaceId, page)
                .map(envoiMapper::toResponse);
        return PagedResponse.fromPage(envoiResponsePage);
    }

    public EnvoiResponse update(Long envoiId, EnvoiUpdateRequest requestBody, Long workspaceId, Long userId) {
        ensureWorkspaceBelongsToUser(workspaceId, userId);
        Envoi envoi = envoiRepository.findByIdAndWorkspace_Id(envoiId, workspaceId)
                .orElseThrow(EnvoiNotFoundException::new);
        envoiMapper.updateFromRequest(envoi, requestBody);
        envoiRepository.save(envoi);
        return envoiMapper.toResponse(envoi);
    }

    @Transactional
    public void delete(Long envoiId, Long workspaceId, Long userId) {
        ensureWorkspaceBelongsToUser(workspaceId, userId);
        envoiRepository.deleteByIdAndWorkspace_Id(envoiId, workspaceId);
    }

    public EnvoiResponse get(Long envoiId, Long workspaceId, Long userId) {
        ensureWorkspaceBelongsToUser(workspaceId, userId);
        Envoi envoi = envoiRepository.findByIdAndWorkspace_Id(envoiId, workspaceId)
                .orElseThrow(EnvoiNotFoundException::new);
        return envoiMapper.toResponse(envoi);
    }

    @Transactional
    public List<PieceResponse> createPieces(Long envoiId, List<PieceCreateRequest> request, Long workspaceId, Long userId) {
        ensureWorkspaceBelongsToUser(workspaceId, userId);
        Envoi envoi = envoiRepository.findByIdAndWorkspace_Id(envoiId, workspaceId)
                .orElseThrow(EnvoiNotFoundException::new);
        List<EnvoiPiece> pieceList = request.stream()
                .map(dto -> {
                    EnvoiPiece piece = pieceMapper.fromPieceCreateRequest(dto);
                    envoi.getPieces().add(piece);
                    piece.setEnvoi(envoi);
                    return piece;
                })
                .collect(Collectors.toList());
        List<EnvoiPiece> savedPieces = envoiPieceRepository.saveAllAndFlush(pieceList);
        return envoiPieceMapper.toResponseList(savedPieces);
    }

    @Transactional
    public List<PieceResponse> updatePieces(Long envoiId, List<PieceUpdateRequest> request, Long workspaceId, Long userId) {
        ensureWorkspaceBelongsToUser(workspaceId, userId);
        Envoi envoi = envoiRepository.findByIdAndWorkspace_Id(envoiId, workspaceId)
                .orElseThrow(EnvoiNotFoundException::new);
        List<Long> ids = request.stream().map(PieceUpdateRequest::id).toList();
        List<EnvoiPiece> envoiPieces = envoiPieceRepository.findAllByEnvoiAndIdIn(envoi, ids);
        Map<Long, EnvoiPiece> pieceMap = envoiPieces.stream().collect(Collectors.toMap(EnvoiPiece::getId, piece -> piece));

        Set<Long> seenIds = new HashSet<>(pieceMap.size());
        return request.stream()
                .map(r -> {
                    EnvoiPiece piece = pieceMap.get(r.id());
                    if (piece == null) throw new EnvoiPieceNotFoundException();
                    if (!seenIds.add(r.id())) throw new DuplicatePieceIdException();
                    envoiPieceMapper.updateFromRequest(piece, r);
                    return envoiPieceMapper.toResponse(piece);
                }).toList();
    }

    @Transactional
    public void deletePieces(Long envoiId, List<Long> pieceIds, Long workspaceId, Long userId) {
        ensureWorkspaceBelongsToUser(workspaceId, userId);
        Envoi envoi = envoiRepository.findByIdAndWorkspace_Id(envoiId, workspaceId)
                .orElseThrow(EnvoiNotFoundException::new);
        envoiPieceRepository.deleteAllByEnvoiAndIdIn(envoi, pieceIds);
    }

    public PagedResponse<PieceResponse> getPiecesPage(Long envoiId, Pageable page, Long workspaceId, Long userId) {
        ensureWorkspaceBelongsToUser(workspaceId, userId);
        Envoi envoi = envoiRepository.findByIdAndWorkspace_Id(envoiId, workspaceId)
                .orElseThrow(EnvoiNotFoundException::new);
        Page<PieceResponse> pieces = envoiPieceRepository.findAllByEnvoi(envoi, page).map(envoiPieceMapper::toResponse);
        return PagedResponse.fromPage(pieces);
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
