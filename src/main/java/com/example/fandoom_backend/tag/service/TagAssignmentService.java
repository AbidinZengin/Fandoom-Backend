package com.example.fandoom_backend.tag.service;

import com.example.fandoom_backend.tag.dto.TagAssignmentRequest;
import com.example.fandoom_backend.tag.dto.TagAssignmentResponse;
import com.example.fandoom_backend.tag.dto.TagFacetOptionResponse;
import com.example.fandoom_backend.tag.entity.TagType;
import com.example.fandoom_backend.tag.entity.TaggableType;

import java.util.List;

public interface TagAssignmentService {
    List<TagAssignmentResponse> listForTarget(TaggableType taggableType, Long taggableId);
    TagAssignmentResponse assign(Long tagId, TagAssignmentRequest request);
    void delete(Long id);

    // Blog hub facet paneli: verilen tip (FORMAT/MOOD/THEME) ve hedef
    // (BLOG) için her tag'in atanma sayısı. TagService'e değil buraya
    // konuldu çünkü döndürdüğü veri (count) TagAssignment kayıtlarının
    // bir agregasyonu — TagService yalnızca Tag'in kendisiyle ilgilenir.
    List<TagFacetOptionResponse> findFacetOptions(TagType type, TaggableType taggableType);
}
