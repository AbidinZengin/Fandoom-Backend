package com.example.fandoom_backend.community.mapper;

import com.example.fandoom_backend.common.util.LocalizedTextResolver;
import com.example.fandoom_backend.community.dto.AdminPortalResponse;
import com.example.fandoom_backend.community.dto.PortalDetailResponse;
import com.example.fandoom_backend.community.dto.PortalRefResponse;
import com.example.fandoom_backend.community.dto.PortalSummaryResponse;
import com.example.fandoom_backend.community.entity.Portal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// name/description: istek diline göre (LocaleConfig / Accept-Language), boşsa diğer dile düşer — GenreMapper ile aynı kalıp.
@Mapper(componentModel = "spring", imports = LocalizedTextResolver.class)
public interface PortalMapper {

    @Mapping(target = "name", expression = "java(LocalizedTextResolver.resolve(portal.getNameTr(), portal.getNameEn()))")
    PortalRefResponse toRef(Portal portal);

    @Mapping(target = "productionSlugs", source = "productionSlugs")
    AdminPortalResponse toAdminResponse(Portal portal, List<String> productionSlugs);

    @Mapping(target = "name", expression = "java(LocalizedTextResolver.resolve(portal.getNameTr(), portal.getNameEn()))")
    @Mapping(target = "description", expression = "java(LocalizedTextResolver.resolve(portal.getDescriptionTr(), portal.getDescriptionEn()))")
    @Mapping(target = "isMember", source = "member")
    @Mapping(target = "productionSlugs", source = "productionSlugs")
    PortalSummaryResponse toSummary(Portal portal, boolean member, List<String> productionSlugs);

    @Mapping(target = "name", expression = "java(LocalizedTextResolver.resolve(portal.getNameTr(), portal.getNameEn()))")
    @Mapping(target = "description", expression = "java(LocalizedTextResolver.resolve(portal.getDescriptionTr(), portal.getDescriptionEn()))")
    @Mapping(target = "isMember", source = "member")
    @Mapping(target = "productionSlugs", source = "productionSlugs")
    PortalDetailResponse toDetail(Portal portal, boolean member, List<String> productionSlugs);
}
