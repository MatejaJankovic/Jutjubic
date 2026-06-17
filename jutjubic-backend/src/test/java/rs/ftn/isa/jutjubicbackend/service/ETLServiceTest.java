package rs.ftn.isa.jutjubicbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.ftn.isa.jutjubicbackend.dto.PopularVideosResponseDTO;
import rs.ftn.isa.jutjubicbackend.model.PopularVideo;
import rs.ftn.isa.jutjubicbackend.repository.PopularVideoRepository;
import rs.ftn.isa.jutjubicbackend.repository.VideoRepository;
import rs.ftn.isa.jutjubicbackend.repository.VideoViewRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ETLServiceTest {

    @Mock private VideoViewRepository videoViewRepository;
    @Mock private PopularVideoRepository popularVideoRepository;
    @Mock private VideoRepository videoRepository;

    @InjectMocks
    private ETLService etlService;

    // Test 10: getLatestPopularVideos vraca prazan Optional kad nema podataka u bazi (mock repository)
    @Test
    void getLatestPopularVideos_returnsEmptyOptionalWhenNoPipelineRunYet() {
        when(popularVideoRepository.findLatest()).thenReturn(Optional.empty());

        Optional<PopularVideosResponseDTO> result = etlService.getLatestPopularVideos();

        assertTrue(result.isEmpty());
        verify(popularVideoRepository).findLatest();
    }

    // Test 11: runETLPipeline sacuva PopularVideo cak i kad nema pregleda (mock sva 3 repositorija)
    @Test
    void runETLPipeline_savesPopularVideoRecordEvenWithNoViews() {
        when(videoViewRepository.findViewsSince(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(popularVideoRepository.save(any(PopularVideo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> etlService.runETLPipeline());

        verify(videoViewRepository).findViewsSince(any(LocalDateTime.class));
        verify(popularVideoRepository).save(any(PopularVideo.class));
        verifyNoInteractions(videoRepository);
    }
}
