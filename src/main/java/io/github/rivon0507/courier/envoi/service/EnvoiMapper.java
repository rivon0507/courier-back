package io.github.rivon0507.courier.envoi.service;

import io.github.rivon0507.courier.envoi.api.EnvoiCreateRequest;
import io.github.rivon0507.courier.envoi.api.EnvoiDetailsResponse;
import io.github.rivon0507.courier.envoi.api.EnvoiResponse;
import io.github.rivon0507.courier.envoi.api.EnvoiUpdateRequest;
import io.github.rivon0507.courier.envoi.domain.Envoi;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = EnvoiPieceMapper.class)
public interface EnvoiMapper {
    @Mapping(target = "workspace", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "pieces", ignore = true)
    Envoi fromCreateRequest(EnvoiCreateRequest dto);

    @Mapping(target = "envoi", source = "entity")
    EnvoiDetailsResponse toDetailsResponse(Envoi entity);

    EnvoiResponse toResponse(Envoi envoi);

    @Mapping(target = "reference", ignore = true)
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateFromRequest(@MappingTarget Envoi envoi, EnvoiUpdateRequest dto);
}
