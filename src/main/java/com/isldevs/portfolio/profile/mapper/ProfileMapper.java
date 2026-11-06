package com.isldevs.portfolio.profile.mapper;

import com.isldevs.portfolio.profile.dto.ProfileResponse;
import com.isldevs.portfolio.profile.entity.Profile;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProfileMapper {
  @Mapping(target = "fullName", expression = "java(text(profile, \"fullName\"))")
  @Mapping(target = "professionalTitle", expression = "java(text(profile, \"professionalTitle\"))")
  @Mapping(target = "headline", expression = "java(text(profile, \"headline\"))")
  @Mapping(target = "shortBio", expression = "java(text(profile, \"shortBio\"))")
  @Mapping(target = "fullBio", expression = "java(text(profile, \"fullBio\"))")
  @Mapping(target = "location", expression = "java(text(profile, \"location\"))")
  @Mapping(target = "specialization", expression = "java(text(profile, \"specialization\"))")
  @Mapping(target = "availability", expression = "java(text(profile, \"availability\"))")
  @Mapping(target = "githubUrl", expression = "java(text(profile, \"githubUrl\"))")
  @Mapping(target = "linkedinUrl", expression = "java(text(profile, \"linkedinUrl\"))")
  @Mapping(target = "publicEmail", expression = "java(text(profile, \"publicEmail\"))")
  @Mapping(target = "portraitUrl", expression = "java(text(profile, \"portraitUrl\"))")
  @Mapping(target = "resumeUrl", expression = "java(text(profile, \"resumeUrl\"))")
  @Mapping(target = "seoTitle", expression = "java(text(profile, \"seoTitle\"))")
  @Mapping(target = "seoDescription", expression = "java(text(profile, \"seoDescription\"))")
  ProfileResponse toDto(Profile profile);

  default String text(Profile profile, String field) {
    Object value = profile.data.get(field);
    return value instanceof String text ? text : null;
  }
}
