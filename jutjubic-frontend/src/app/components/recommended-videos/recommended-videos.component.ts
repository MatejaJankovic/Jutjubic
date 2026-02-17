import { Component, EventEmitter, Input, OnInit, Output, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WatchPartyService } from '../../services/watch-party.service';
import { VideoService } from '../../services/video.service';
import { Video } from '../../models/video.model';
import { environment } from '../../env/environment';

@Component({
  selector: 'app-recommended-videos',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './recommended-videos.component.html',
  styleUrls: ['./recommended-videos.component.scss']
})
export class RecommendedVideosComponent implements OnInit {
  @Input() currentVideoId: number | null = null;
  @Input() isOwner = false;
  @Output() videoSelected = new EventEmitter<Video>();

  videos: Video[] = [];
  loading = false;
  loadingMore = false;
  hasMore = true;
  currentPage = 0;
  pageSize = 10;
  error = '';
  environment = environment;

  constructor(
    private watchPartyService: WatchPartyService,
    private videoService: VideoService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    this.loadRecommendedVideos();
  }

  loadRecommendedVideos(): void {
    if (this.loading) return;

    this.loading = true;
    this.error = '';
    this.cdr.detectChanges();

    this.watchPartyService.getRecommendedVideos(this.currentVideoId ?? undefined, 0, this.pageSize)
      .subscribe({
        next: (response) => {
          this.ngZone.run(() => {
            this.videos = response.videos;
            this.hasMore = response.hasNext;
            this.currentPage = 0;
            this.loading = false;
            this.cdr.detectChanges();
          });
        },
        error: (err) => {
          this.ngZone.run(() => {
            console.error('Error loading recommended videos:', err);
            this.error = 'Failed to load recommended videos';
            this.loading = false;
            this.cdr.detectChanges();
          });
        }
      });
  }

  loadMore(): void {
    if (this.loadingMore || !this.hasMore) return;

    this.loadingMore = true;
    this.cdr.detectChanges();

    const nextPage = this.currentPage + 1;
    this.watchPartyService.getRecommendedVideos(this.currentVideoId ?? undefined, nextPage, this.pageSize)
      .subscribe({
        next: (response) => {
          this.ngZone.run(() => {
            this.videos = [...this.videos, ...response.videos];
            this.hasMore = response.hasNext;
            this.currentPage = nextPage;
            this.loadingMore = false;
            this.cdr.detectChanges();
          });
        },
        error: (err) => {
          this.ngZone.run(() => {
            console.error('Error loading more videos:', err);
            this.loadingMore = false;
            this.cdr.detectChanges();
          });
        }
      });
  }

  onVideoClick(video: Video): void {
    if (this.isOwner) {
      this.videoSelected.emit(video);
    }
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

