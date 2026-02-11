package io.github.rivon0507.courier.reception.service;

import io.github.rivon0507.courier.common.api.PieceCreateRequest;
import io.github.rivon0507.courier.common.api.PieceResponse;
import io.github.rivon0507.courier.common.api.PieceUpdateRequest;
import io.github.rivon0507.courier.reception.domain.ReceptionPiece;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReceptionPieceMapper {
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "reception", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ReceptionPiece fromPieceCreateRequest(PieceCreateRequest dto);

    List<PieceResponse> toResponseList(List<ReceptionPiece> entities);

    PieceResponse toResponse(ReceptionPiece dto);

    @Mapping(target = "id", ignore = true)
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateFromRequest(@MappingTarget ReceptionPiece entity, PieceUpdateRequest dto);
}
