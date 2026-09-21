package com.example.fandoom_backend.common.config;

import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.mapper.ThreadMapper;
import com.example.fandoom_backend.community.repository.ThreadBookmarkRepository;
import com.example.fandoom_backend.community.repository.ThreadLikeRepository;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import com.example.fandoom_backend.community.repository.ThreadTagRepository;
import com.example.fandoom_backend.community.service.PortalService;
import com.example.fandoom_backend.community.service.ThreadMediaService;
import com.example.fandoom_backend.community.service.ThreadService;
import com.example.fandoom_backend.community.service.ThreadServiceImpl;
import com.example.fandoom_backend.community.service.UserProfileService;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import com.example.fandoom_backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Gerçek RedisCacheManager + CacheErrorConfig ile: @Cacheable(sync=true) bir metodun fırlattığı iş exception'ı
// (ResourceNotFoundException -> 404) cache katmanında sarılıp 500'e dönüşmemeli. Redis ERİŞİLEMEZ (kapalı port):
// üretimdeki "Redis kapalı" durumunun aynısı.
@SpringJUnitConfig(CacheNotFoundPropagationTest.Cfg.class)
class CacheNotFoundPropagationTest {

    @Configuration
    @Import({RedisConfig.class, CacheErrorConfig.class})
    static class Cfg {
        @Bean LettuceConnectionFactory redisConnectionFactory() {
            RedisStandaloneConfiguration server = new RedisStandaloneConfiguration("localhost", 6390); // dinleyen yok
            LettuceClientConfiguration client = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofMillis(300)).build();
            return new LettuceConnectionFactory(server, client);
        }

        @Bean ThreadRepository threadRepository() { return mock(ThreadRepository.class); }

        @Bean ThreadServiceImpl threadService(ThreadRepository repo) {
            return new ThreadServiceImpl(repo, mock(ThreadTagRepository.class), mock(ThreadLikeRepository.class),
                    mock(ThreadBookmarkRepository.class), mock(ThreadMapper.class), mock(MovieService.class),
                    mock(SeriesService.class), mock(UserService.class), mock(UserProfileService.class),
                    mock(ThreadMediaService.class), mock(PortalService.class));
        }
    }

    @Autowired ThreadService service;
    @Autowired ThreadRepository threadRepository;


    @Test
    void getById_missingThread_redisDown_stillThrowsResourceNotFound() {
        when(threadRepository.findVisibleById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(999L, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @SuppressWarnings("deprecation")
    void getBySlug_missingThread_redisDown_stillThrowsResourceNotFound() {
        when(threadRepository.findVisibleBySlug("yok")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getBySlug("yok", null)).isInstanceOf(ResourceNotFoundException.class);
    }
}
