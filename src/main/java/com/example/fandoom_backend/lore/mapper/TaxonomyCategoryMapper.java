package com.example.fandoom_backend.lore.mapper;

import com.example.fandoom_backend.lore.dto.TaxonomyCategoryResponse;
import com.example.fandoom_backend.lore.entity.TaxonomyCategory;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TaxonomyCategoryMapper {
    TaxonomyCategoryResponse toResponse(TaxonomyCategory category);
    List<TaxonomyCategoryResponse> toResponseList(List<TaxonomyCategory> categories);
}
