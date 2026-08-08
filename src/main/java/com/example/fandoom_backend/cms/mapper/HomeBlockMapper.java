package com.example.fandoom_backend.cms.mapper;

import com.example.fandoom_backend.cms.dto.HomeBlockResponse;
import com.example.fandoom_backend.cms.entity.HomeBlock;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HomeBlockMapper {
    HomeBlockResponse toResponse(HomeBlock homeBlock);
    List<HomeBlockResponse> toResponseList(List<HomeBlock> homeBlocks);
}
