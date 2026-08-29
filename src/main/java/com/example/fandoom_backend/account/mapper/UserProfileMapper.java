package com.example.fandoom_backend.account.mapper;

import com.example.fandoom_backend.account.dto.ProfileStats;
import com.example.fandoom_backend.account.dto.UserProfileResponse;
import com.example.fandoom_backend.account.entity.UserProfile;
import org.mapstruct.Mapper;

// username/stats: UserProfile entity'sinde yok (username user/'a ait,
// stats hesaplanmış değer) — MapStruct çoklu-parametre desteğiyle aynı
// isimli hedef alanlara otomatik eşlenir.
@Mapper(componentModel = "spring")
public interface UserProfileMapper {
    UserProfileResponse toResponse(UserProfile profile, String username, ProfileStats stats);
}
