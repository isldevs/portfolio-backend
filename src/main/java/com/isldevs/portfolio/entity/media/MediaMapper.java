package com.isldevs.portfolio.entity.media;

import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MediaMapper {
  @Mapping(target = "url", expression = "java(url(asset))")
  MediaService.Asset toDto(MediaAsset asset);

  default String url(MediaAsset asset) {
    return asset.visibility == MediaService.Visibility.PUBLIC
        ? "/api/v1/public/media/" + asset.id
        : "/api/v1/admin/media/" + asset.id + "/content";
  }
}
