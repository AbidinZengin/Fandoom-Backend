package com.example.fandoom_backend.community;

import com.example.fandoom_backend.community.HotScoreRangeUpdater.IdRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotScoreJobTest {

    @Mock private HotScoreRangeUpdater updater;

    @Test
    void walksHalfOpenIdWindows_coveringMaxIdExactlyOnce() {
        when(updater.publishedIdRange()).thenReturn(Optional.of(new IdRange(1, 25)));
        when(updater.recalculate(anyLong(), anyLong(), any())).thenReturn(10);

        new HotScoreJob(updater, 10).recalculateHotScores();

        verify(updater).recalculate(eq(1L), eq(11L), any());   // [1, 11)
        verify(updater).recalculate(eq(11L), eq(21L), any());  // [11, 21)
        verify(updater).recalculate(eq(21L), eq(31L), any());  // [21, 31) -> 25'i kapsar, biter
        verify(updater, times(3)).recalculate(anyLong(), anyLong(), any());
    }

    @Test
    void maxIdOnWindowBoundary_isStillCovered() {
        // Pencere [1, 11) -> maxId=11 dışarıda kalır; ikinci pencere [11, 21) onu kapsamalı.
        when(updater.publishedIdRange()).thenReturn(Optional.of(new IdRange(1, 11)));

        new HotScoreJob(updater, 10).recalculateHotScores();

        verify(updater).recalculate(eq(1L), eq(11L), any());
        verify(updater).recalculate(eq(11L), eq(21L), any());
        verify(updater, times(2)).recalculate(anyLong(), anyLong(), any());
    }

    @Test
    void noPublishedThreads_runsNoUpdate() {
        when(updater.publishedIdRange()).thenReturn(Optional.empty());

        new HotScoreJob(updater, 10).recalculateHotScores();

        verify(updater, never()).recalculate(anyLong(), anyLong(), any());
    }

    @Test
    void hugeWindow_collapsesToSingleStatement_withoutOverflowLoop() {
        when(updater.publishedIdRange()).thenReturn(Optional.of(new IdRange(1, 1_000_000)));

        new HotScoreJob(updater, Long.MAX_VALUE).recalculateHotScores();

        verify(updater, times(1)).recalculate(eq(1L), eq(Long.MAX_VALUE), any());
    }

    @Test
    void usesSingleNowForAllWindows() {
        when(updater.publishedIdRange()).thenReturn(Optional.of(new IdRange(1, 5)));

        new HotScoreJob(updater, 2).recalculateHotScores();

        ArgumentCaptor<LocalDateTime> nows = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(updater, times(3)).recalculate(anyLong(), anyLong(), nows.capture());
        assertThat(nows.getAllValues()).containsOnly(nows.getAllValues().get(0));
    }

    @Test
    void windowFailure_isSwallowedSoCacheEvictStillRuns_andStopsTheRun() {
        when(updater.publishedIdRange()).thenReturn(Optional.of(new IdRange(1, 100)));
        when(updater.recalculate(eq(1L), anyLong(), any())).thenReturn(5);
        when(updater.recalculate(eq(11L), anyLong(), any())).thenThrow(new IllegalStateException("db down"));

        assertThatCode(() -> new HotScoreJob(updater, 10).recalculateHotScores()).doesNotThrowAnyException();

        verify(updater, times(2)).recalculate(anyLong(), anyLong(), any());
    }

    @Test
    void invalidWindowSize_failsFastAtStartup() {
        assertThatThrownBy(() -> new HotScoreJob(updater, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
