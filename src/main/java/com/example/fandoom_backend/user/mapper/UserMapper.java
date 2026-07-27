package com.example.fandoom_backend.user.mapper;

import com.example.fandoom_backend.user.dto.UserDetailResponse;
import com.example.fandoom_backend.user.dto.UserSummaryResponse;
import com.example.fandoom_backend.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "premiumActive",
            expression = "java(user.getPremiumExpiresAt() != null && user.getPremiumExpiresAt().isAfter(java.time.LocalDateTime.now()))")
    UserSummaryResponse toSummaryResponse(User user);

    @Mapping(target = "premiumActive",
            expression = "java(user.getPremiumExpiresAt() != null && user.getPremiumExpiresAt().isAfter(java.time.LocalDateTime.now()))")
    UserDetailResponse toDetailResponse(User user);
}
