package com.example.fandoom_backend.community.mapper;

import com.example.fandoom_backend.community.dto.ProfileStats;
import com.example.fandoom_backend.community.dto.UserProfileResponse;
import com.example.fandoom_backend.community.entity.UserProfile;
import org.mapstruct.Mapper;

// username/stats: UserProfile entity'sinde yok (username user/'a ait,
// stats hesaplanmış değer) — MapStruct çoklu-parametre desteğiyle aynı
// isimli hedef alanlara otomatik eşlenir.
@Mapper(componentModel = "spring")
public interface UserProfileMapper {
    UserProfileResponse toResponse(UserProfile profile, String username, ProfileStats stats);
}
