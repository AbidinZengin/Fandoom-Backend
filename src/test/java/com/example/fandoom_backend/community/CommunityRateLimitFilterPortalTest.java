package com.example.fandoom_backend.community;

import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.entity.User;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// Id tabanlı thread uçları (/threads/{id}/comments) da mevcut comment-create ve thread-create bucket'larına girer.
class CommunityRateLimitFilterPortalTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(long id) {
        User user = User.builder().id(id).username("u" + id).password("x").role(Role.USER).emailVerified(true).build();
        CustomUserDetails principal = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static MockHttpServletRequest post(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRequestURI(uri);
        return request;
    }

    @Test
    void shouldFilter_idAndSlugCommentCreate_threadCreate_butNotLikeOrGet() {
        CommunityRateLimitFilter filter = new CommunityRateLimitFilter(JsonMapper.builder().build());

        assertThat(filter.shouldNotFilter(post("/api/community/threads/123/comments"))).isFalse();
        assertThat(filter.shouldNotFilter(post("/api/community/threads/eski-slug/comments"))).isFalse();
        assertThat(filter.shouldNotFilter(post("/api/community/threads"))).isFalse();
        assertThat(filter.shouldNotFilter(post("/api/community/threads/123/like"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/community/threads/123/comments")))
                .isTrue();
    }

    @Test
    void idBasedCommentCreate_shares_theCommentBucket_with_slugBasedOne() throws Exception {
        CommunityRateLimitFilter filter = new CommunityRateLimitFilter(JsonMapper.builder().build());
        FilterChain chain = mock(FilterChain.class);
        loginAs(77L);

        int tooMany = 0;
        for (int i = 0; i < 31; i++) {
            // id ve slug yolları dönüşümlü: aynı kullanıcı+bucket paylaşılır -> 31. istek 30 sınırını aşar (429)
            String uri = i % 2 == 0 ? "/api/community/threads/5/comments" : "/api/community/threads/bir-slug/comments";
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(post(uri), response, chain);
            if (response.getStatus() == 429) {
                tooMany++;
            }
        }

        assertThat(tooMany).isEqualTo(1);
    }

    @Test
    void portalJoinAndLeave_shareOneBucket_limitedTo60PerHour() throws Exception {
        CommunityRateLimitFilter filter = new CommunityRateLimitFilter(JsonMapper.builder().build());
        FilterChain chain = mock(FilterChain.class);
        loginAs(88L);

        assertThat(filter.shouldNotFilter(post("/api/community/portals/x/join"))).isFalse();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("DELETE", "/api/community/portals/x/join"))).isFalse();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/community/portals/x/join"))).isTrue();

        int tooMany = 0;
        for (int i = 0; i < 61; i++) {
            // join (POST) ve leave (DELETE) dönüşümlü: aynı kullanıcı+bucket -> 61. istek 429
            String uri = "/api/community/portals/p" + (i % 3) + "/join";
            MockHttpServletRequest request = i % 2 == 0 ? post(uri) : new MockHttpServletRequest("DELETE", uri);
            request.setRequestURI(uri);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            if (response.getStatus() == 429) {
                tooMany++;
            }
        }

        assertThat(tooMany).isEqualTo(1);
    }
}
