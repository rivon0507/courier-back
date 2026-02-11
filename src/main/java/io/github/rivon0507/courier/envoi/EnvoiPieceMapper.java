package io.github.rivon0507.courier.envoi;

import io.github.rivon0507.courier.common.api.PieceCreateRequest;
import io.github.rivon0507.courier.common.api.PieceResponse;
import io.github.rivon0507.courier.common.api.PieceUpdateRequest;
import io.github.rivon0507.courier.envoi.domain.EnvoiPiece;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EnvoiPieceMapper {
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "envoi", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    EnvoiPiece fromPieceCreateRequest(PieceCreateRequest dto);

    List<PieceResponse> toResponseList(List<EnvoiPiece> entities);

    @Mapping(target = "id", ignore = true)
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateFromRequest(@MappingTarget EnvoiPiece piece, PieceUpdateRequest dto);

    PieceResponse toResponse(EnvoiPiece entity);
}
