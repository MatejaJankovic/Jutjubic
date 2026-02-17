import { Component, Input, OnInit, OnChanges, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { Video } from '../../models/video.model';
import { environment } from '../../env/environment';

@Component({
  selector: 'app-video-recommendations',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './video-recommendations.component.html',
  styleUrls: ['./video-recommendations.component.scss']
})
export class VideoRecommendationsComponent implements OnInit, OnChanges {
  @Input() currentVideoId: number | null = null;

  videos: Video[] = [];
  loading = false;
  loadingMore = false;
  hasMore = true;
  currentPage = 0;
  pageSize = 10;
  error = '';
  environment = environment;

  constructor(
    private videoService: VideoService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadRecommendedVideos();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['currentVideoId'] && !changes['currentVideoId'].firstChange) {
      // Reset and reload when video changes
      this.videos = [];
      this.currentPage = 0;
      this.hasMore = true;
      this.loadRecommendedVideos();
    }
  }

  loadRecommendedVideos(): void {
    if (this.loading) return;

    this.loading = true;
    this.error = '';
    this.cdr.detectChanges();

    this.videoService.getRecommendedVideos(this.currentVideoId ?? undefined, 0, this.pageSize)
      .subscribe({
        next: (response) => {
          this.videos = response.videos;
          this.hasMore = response.hasNext;
          this.currentPage = 0;
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error loading recommended videos:', err);
          this.error = 'Greška pri učitavanju preporučenih video snimaka';
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
  }

  loadMore(): void {
    if (this.loadingMore || !this.hasMore) return;

    this.loadingMore = true;
    this.cdr.detectChanges();

    const nextPage = this.currentPage + 1;
    this.videoService.getRecommendedVideos(this.currentVideoId ?? undefined, nextPage, this.pageSize)
      .subscribe({
        next: (response) => {
          this.videos = [...this.videos, ...response.videos];
          this.hasMore = response.hasNext;
          this.currentPage = nextPage;
          this.loadingMore = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error loading more videos:', err);
          this.loadingMore = false;
          this.cdr.detectChanges();
        }
      });
  }

  formatViewCount(count: number): string {
    return this.videoService.formatViewCount(count);
  }

  formatRelativeTime(date: string): string {
    return this.videoService.formatRelativeTime(date);
  }

  formatDuration(seconds: number): string {
    return this.videoService.formatDuration(seconds);
  }

  getChannelInitials(video: Video): string {
    return (video.userFirstName?.charAt(0) || '') + (video.userLastName?.charAt(0) || '');
  }
}

