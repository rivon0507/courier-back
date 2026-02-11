package io.github.rivon0507.courier.reception.service;

import io.github.rivon0507.courier.reception.api.ReceptionCreateRequest;
import io.github.rivon0507.courier.reception.api.ReceptionDetailsResponse;
import io.github.rivon0507.courier.reception.api.ReceptionResponse;
import io.github.rivon0507.courier.reception.api.ReceptionUpdateRequest;
import io.github.rivon0507.courier.reception.domain.Reception;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ReceptionMapper {
    @Mapping(target = "workspace", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "pieces", ignore = true)
    Reception fromCreateRequest(ReceptionCreateRequest dto);

    @Mapping(target = "reception", source = "entity")
    ReceptionDetailsResponse toDetailsResponse(Reception entity);

    ReceptionResponse toResponse(Reception entity);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateFromRequest(@MappingTarget Reception entity, ReceptionUpdateRequest dto);
}
