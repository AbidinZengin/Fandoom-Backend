package com.example.fandoom_backend.blog.service;

import com.example.fandoom_backend.blog.dto.BlogSearchResult;
import com.example.fandoom_backend.blog.entity.BlogStatus;
import com.example.fandoom_backend.blog.repository.BlogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogQueryServiceImplQuickSearchTest {

    @Mock
    private BlogRepository blogRepository;

    @InjectMocks
    private BlogQueryServiceImpl blogQueryService;

    @Test
    void quickSearch_onlyPublished_neverDrafts() {
        BlogSearchResult blog = new BlogSearchResult(8L, "dune-ekoloji", "Dune ecology", null, null, null, null);
        when(blogRepository.quickSearch(BlogStatus.PUBLISHED, "%dune%", "dune%", PageRequest.of(0, 5)))
                .thenReturn(List.of(blog));

        List<BlogSearchResult> result = blogQueryService.quickSearch("dune", 5);

        assertThat(result).containsExactly(blog);
    }
}
