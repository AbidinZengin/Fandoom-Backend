package com.example.fandoom_backend.video.mapper;

import com.example.fandoom_backend.video.dto.VideoResponse;
import com.example.fandoom_backend.video.entity.Video;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VideoMapper {

    VideoResponse toResponse(Video video);

    List<VideoResponse> toResponseList(List<Video> videos);
}
