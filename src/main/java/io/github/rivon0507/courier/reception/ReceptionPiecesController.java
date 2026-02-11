package io.github.rivon0507.courier.reception;

import io.github.rivon0507.courier.common.annotation.CurrentUserId;
import io.github.rivon0507.courier.common.api.PieceCreateRequest;
import io.github.rivon0507.courier.common.api.PieceResponse;
import io.github.rivon0507.courier.common.api.PieceUpdateRequest;
import io.github.rivon0507.courier.common.pagination.PagedResponse;
import io.github.rivon0507.courier.reception.service.ReceptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces/{workspaceId}/receptions/{receptionId}/pieces")
@RequiredArgsConstructor
class ReceptionPiecesController {

    private final ReceptionService receptionService;

    @PostMapping
    public ResponseEntity<List<PieceResponse>> createPieces(
            @PathVariable Long receptionId,
            @Valid @RequestBody List<PieceCreateRequest> requestBody,
            @PathVariable Long workspaceId,
            @CurrentUserId Long userId) {

        return ResponseEntity.ok(receptionService.createPieces(receptionId, requestBody, workspaceId, userId));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<PieceResponse>> getPiecesPage(
            @PageableDefault(sort = "designation") Pageable page,
            @PathVariable Long receptionId,
            @PathVariable Long workspaceId,
            @CurrentUserId Long userId) {

        return ResponseEntity.ok(receptionService.getPiecesPage(receptionId, page, workspaceId, userId));
    }

    @PutMapping
    public ResponseEntity<List<PieceResponse>> updatePieces(
            @PathVariable Long receptionId,
            @Valid @RequestBody List<PieceUpdateRequest> requestBody,
            @PathVariable Long workspaceId,
            @CurrentUserId Long userId) {

        return ResponseEntity.ok(receptionService.updatePieces(receptionId, requestBody, workspaceId, userId));
    }

    @DeleteMapping
    public ResponseEntity<Void> deletePieces(
            @PathVariable Long receptionId,
            @RequestParam(name = "ids") List<Long> ids,
            @PathVariable Long workspaceId,
            @CurrentUserId Long userId) {

        receptionService.deletePieces(receptionId, ids, workspaceId, userId);
        return ResponseEntity.noContent().build();
    }
}
