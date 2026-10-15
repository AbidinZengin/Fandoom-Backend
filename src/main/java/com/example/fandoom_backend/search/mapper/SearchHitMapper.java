package com.example.fandoom_backend.search.mapper;

import com.example.fandoom_backend.blog.dto.BlogSearchResult;
import com.example.fandoom_backend.common.util.LocalizedTextResolver;
import com.example.fandoom_backend.community.dto.PortalSearchResult;
import com.example.fandoom_backend.movie.dto.MovieSearchResult;
import com.example.fandoom_backend.search.dto.SearchHit;
import com.example.fandoom_backend.series.dto.SeriesSearchResult;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;

// Modüllerin kendi arama kutularını ortak SearchHit'e çevirir. Dil burada uygulanır ve AÇIKÇA parametre olarak gelir:
// çağıran (SearchServiceImpl) dili isteğin kendi thread'inde okur — paralel thread'ler LocaleContextHolder'ı görmez.
// MapStruct değil: her tür için alan eşlemesi + dil çözümü + yıl türetme var, düz kod daha okunur.
@Component
public class SearchHitMapper {

    public SearchHit fromMovie(MovieSearchResult m, Locale locale) {
        return new SearchHit(m.id(), m.slug(), LocalizedTextResolver.resolve(m.titleTr(), m.title(), locale),
                year(m.releaseDate()), m.posterUrl());
    }

    public SearchHit fromSeries(SeriesSearchResult s, Locale locale) {
        return new SearchHit(s.id(), s.slug(), LocalizedTextResolver.resolve(s.titleTr(), s.title(), locale),
                year(s.firstAirDate()), s.posterUrl());
    }

    public SearchHit fromPortal(PortalSearchResult p, Locale locale) {
        return new SearchHit(p.id(), p.slug(), LocalizedTextResolver.resolve(p.nameTr(), p.nameEn(), locale),
                null, p.iconUrl());
    }

    public SearchHit fromBlog(BlogSearchResult b, Locale locale) {
        return new SearchHit(b.id(), b.slug(), LocalizedTextResolver.resolve(b.titleTr(), b.title(), locale),
                LocalizedTextResolver.resolve(b.kickerTr(), b.kicker(), locale), b.imageUrl());
    }

    private static String year(LocalDate date) {
        return date == null ? null : String.valueOf(date.getYear());
    }
}
