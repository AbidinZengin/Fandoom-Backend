package com.example.fandoom_backend.series.mapper;

import com.example.fandoom_backend.common.util.LocalizedTextResolver;
import com.example.fandoom_backend.series.dto.SeriesHeroBlockResponse;
import com.example.fandoom_backend.series.entity.SeriesHeroBlock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", imports = LocalizedTextResolver.class)
public interface SeriesHeroBlockMapper {

    @Mapping(target = "text", expression = "java(LocalizedTextResolver.resolve(block.getTextTr(), block.getText()))")
    SeriesHeroBlockResponse toResponse(SeriesHeroBlock block);

    List<SeriesHeroBlockResponse> toResponseList(List<SeriesHeroBlock> blocks);
}
