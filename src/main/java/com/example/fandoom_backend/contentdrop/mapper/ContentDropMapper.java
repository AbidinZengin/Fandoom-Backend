package com.example.fandoom_backend.contentdrop.mapper;

import com.example.fandoom_backend.contentdrop.dto.ContentDropBlockResponse;
import com.example.fandoom_backend.contentdrop.dto.ContentDropDetailResponse;
import com.example.fandoom_backend.contentdrop.dto.ContentDropSummaryResponse;
import com.example.fandoom_backend.contentdrop.dto.ContentDropTagResponse;
import com.example.fandoom_backend.contentdrop.entity.ContentDrop;
import com.example.fandoom_backend.contentdrop.entity.ContentDropBlock;
import com.example.fandoom_backend.contentdrop.entity.ContentDropTag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ContentDropMapper {

    ContentDropSummaryResponse toSummaryResponse(ContentDrop contentDrop);

    List<ContentDropSummaryResponse> toSummaryResponseList(List<ContentDrop> contentDrops);

    ContentDropBlockResponse toBlockResponse(ContentDropBlock block);

    ContentDropTagResponse toTagResponse(ContentDropTag tag);

    // relatedContentDrops entity'nin kendi alanı değil, servis katmanında (kürasyon+
    // algoritmik merdiven birleşimi) hesaplanıp ikinci parametreyle geçirilir.
    @Mapping(target = "relatedContentDrops", source = "related")
    ContentDropDetailResponse toDetailResponse(ContentDrop contentDrop, List<ContentDropSummaryResponse> related);
}
