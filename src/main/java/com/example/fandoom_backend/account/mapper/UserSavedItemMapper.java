package com.example.fandoom_backend.account.mapper;

import com.example.fandoom_backend.account.dto.UserSavedItemResponse;
import com.example.fandoom_backend.account.entity.UserSavedItem;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserSavedItemMapper {
    UserSavedItemResponse toResponse(UserSavedItem item);

    List<UserSavedItemResponse> toResponseList(List<UserSavedItem> items);
}
