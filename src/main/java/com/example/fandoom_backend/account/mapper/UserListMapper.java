package com.example.fandoom_backend.account.mapper;

import com.example.fandoom_backend.account.dto.UserListDetailResponse;
import com.example.fandoom_backend.account.dto.UserListSummaryResponse;
import com.example.fandoom_backend.account.dto.UserSavedItemResponse;
import com.example.fandoom_backend.account.entity.UserList;
import com.example.fandoom_backend.common.dto.PageResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

// UserList.isPublic/isPinned zaten "is" ile başladığı için Lombok'un ürettiği
// erişimciler isPublic()/setPublic() ve isPinned()/setPinned() olur — MapStruct
// bunlardan "public"/"pinned" property adını türetir, record hedefindeki
// literal "isPublic"/"isPinned" ile otomatik eşleşmez. Açık @Mapping şart,
// yoksa alanlar sessizce hep false döner (derleme zamanı warning, çalışma
// zamanı bug).
@Mapper(componentModel = "spring")
public interface UserListMapper {

    @Mapping(target = "isPublic", source = "list.public")
    @Mapping(target = "isPinned", source = "list.pinned")
    UserListSummaryResponse toSummaryResponse(UserList list, long itemCount);

    @Mapping(target = "isPublic", source = "list.public")
    @Mapping(target = "isPinned", source = "list.pinned")
    UserListDetailResponse toDetailResponse(UserList list, PageResponse<UserSavedItemResponse> items);
}
