import { Component, OnInit, OnDestroy, HostListener, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { Video, VideoPageResponse } from '../../models/video.model';
import { Subscription } from 'rxjs';
import { environment } from '../../env/environment';

@Component({
  selector: 'app-trending',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './trending.component.html',
  styleUrls: ['./trending.component.scss'],
})
export class TrendingComponent implements OnInit, OnDestroy {
  videos: Video[] = [];
  loading = false;
  currentPage = 0;
  totalPages = 0;
  hasNext = false;
  environment = environment;
  private subscription = new Subscription();

  constructor(
    private videoService: VideoService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Odmah učitaj videe
    this.loadVideos();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  private refreshVideos(): void {
    this.currentPage = 0;
    this.videos = [];
    this.loading = false;
    this.loadVideos();
  }

  loadVideos(): void {
    if (this.loading) return;
    this.loading = true;

    this.subscription.add(
      this.videoService.getTrendingVideos(this.currentPage, 12).subscribe({
        next: (response: VideoPageResponse) => {
          this.videos = [...this.videos, ...response.videos];
          this.totalPages = response.totalPages;
          this.hasNext = response.hasNext;
          this.loading = false;
          this.cdr.detectChanges(); // Prisili Angular da osveži UI
        },
        error: () => {
          this.loading = false;
          this.cdr.detectChanges();
        }
      })
    );
  }

  loadMore(): void {
    if (!this.hasNext || this.loading) return;
    this.currentPage++;
    this.loadVideos();
  }

  @HostListener('window:scroll')
  onScroll(): void {
    const scrollPosition = window.innerHeight + window.scrollY;
    const documentHeight = document.documentElement.scrollHeight;

    if (scrollPosition >= documentHeight - 500 && this.hasNext && !this.loading) {
      this.loadMore();
    }
  }

  onVideoClick(video: Video): void {
    this.router.navigate(['/watch', video.id], { state: { video } });
  }

  formatDuration(seconds: number): string {
    return this.videoService.formatDuration(seconds);
  }

  formatViewCount(count: number): string {
    return this.videoService.formatViewCount(count);
  }

  formatRelativeTime(date: string): string {
    return this.videoService.formatRelativeTime(date);
  }

  getChannelInitials(video: Video): string {
    return (video.userFirstName?.charAt(0) || '') + (video.userLastName?.charAt(0) || '');
  }

  scrollToTop(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}
