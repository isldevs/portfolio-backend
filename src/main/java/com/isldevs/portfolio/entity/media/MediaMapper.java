package com.isldevs.portfolio.entity.media;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MediaMapper {
    @Mapping(target = "url", expression = "java(url(asset))")
    Asset toDto(Media asset);

    default String url(Media asset) {
        return asset.visibility == Visibility.PUBLIC
                ? "/api/v1/public/media/" + asset.id
                : "/api/v1/admin/media/" + asset.id + "/content";
    }
}
