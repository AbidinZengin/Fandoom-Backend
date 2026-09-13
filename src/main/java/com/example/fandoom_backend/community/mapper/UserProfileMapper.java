package com.example.fandoom_backend.community.mapper;

import com.example.fandoom_backend.community.dto.ProfileStats;
import com.example.fandoom_backend.community.dto.UserProfileResponse;
import com.example.fandoom_backend.community.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

// username/stats/followerCount/followingCount/isFollowing: UserProfile
// entity'sinde yok (username user/'a, follow sayaçları account/UserFollowService'e
// ait, stats hesaplanmış değer) — MapStruct çoklu-parametre desteğiyle aynı
// isimli hedef alanlara otomatik eşlenir. "id" ise explicit mapleniyor:
// UserProfile'ın kendi PK'sı (profile.id) DEĞİL, userId parametresi (hedef
// kullanıcının User.id'si — bkz. UserProfileResponse javadoc) hedefe yazılır.
@Mapper(componentModel = "spring")
public interface UserProfileMapper {
    @Mapping(target = "id", source = "userId")
    UserProfileResponse toResponse(UserProfile profile, Long userId, String username, ProfileStats stats,
                                    long followerCount, long followingCount, boolean isFollowing);
}
