import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
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
  totalElements = 0;
  pageSize = 12; // 3 reda x 4 kolone
  environment = environment;
  private subscription = new Subscription();

  constructor(
    private videoService: VideoService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadVideos();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  loadVideos(): void {
    if (this.loading) return;
    this.loading = true;
    this.videos = []; // Clear existing videos

    this.subscription.add(
      this.videoService.getTrendingVideos(this.currentPage, this.pageSize).subscribe({
        next: (response: VideoPageResponse) => {
          this.videos = response.videos;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;
          this.loading = false;
          this.scrollToTop();
          this.cdr.detectChanges();
        },
        error: () => {
          this.loading = false;
          this.cdr.detectChanges();
        }
      })
    );
  }

  // Pagination navigation methods
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      this.currentPage = page;
      this.loadVideos();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadVideos();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadVideos();
    }
  }

  goToFirstPage(): void {
    if (this.currentPage !== 0) {
      this.currentPage = 0;
      this.loadVideos();
    }
  }

  goToLastPage(): void {
    const lastPage = this.totalPages - 1;
    if (this.currentPage !== lastPage) {
      this.currentPage = lastPage;
      this.loadVideos();
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPagesToShow = 5;

    if (this.totalPages <= maxPagesToShow) {
      for (let i = 0; i < this.totalPages; i++) {
        pages.push(i);
      }
    } else {
      let startPage = Math.max(0, this.currentPage - 2);
      let endPage = Math.min(this.totalPages - 1, this.currentPage + 2);

      if (this.currentPage < 2) {
        endPage = Math.min(this.totalPages - 1, 4);
      }
      if (this.currentPage > this.totalPages - 3) {
        startPage = Math.max(0, this.totalPages - 5);
      }

      for (let i = startPage; i <= endPage; i++) {
        pages.push(i);
      }
    }

    return pages;
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

