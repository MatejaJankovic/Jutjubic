import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { Video, VideoPageResponse } from '../../models/video.model';
import { Subscription } from 'rxjs';
import { map, skip } from 'rxjs/operators';
import { environment } from '../../env/environment';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
})
export class HomeComponent implements OnInit, OnDestroy {
  videos: Video[] = [];
  loading = false;
  currentPage = 0;
  totalPages = 0;
  totalElements = 0;
  pageSize = 12; // 3 reda x 4 kolone
  searchQuery = '';
  environment = environment;
  activeVideo: Video | null = null;
  private subscription = new Subscription();

  constructor(
    private videoService: VideoService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.searchQuery = this.route.snapshot.queryParams['search'] || '';
    this.loadVideos();

    this.subscription.add(
      this.route.queryParams.pipe(
        skip(1),
        map(params => params['search'] || '')
      ).subscribe(searchQuery => {
        this.searchQuery = searchQuery;
        this.currentPage = 0;
        this.loadVideos();
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  onVideoClick(video: Video): void {
    this.router.navigate(
      ['/watch', video.id],
      { state: { video } }
    );
  }

  closeVideoModal(): void {
    this.activeVideo = null;
  }

  loadVideos(): void {
    if (this.loading) return;
    this.loading = true;
    this.videos = []; // Clear existing videos

    const request = this.searchQuery
      ? this.videoService.searchVideos(this.searchQuery, this.currentPage, this.pageSize)
      : this.videoService.getAllVideos(this.currentPage, this.pageSize);

    this.subscription.add(
      request.subscribe({
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

  playVideo(event: Event) {
    const video = event.target as HTMLVideoElement;
    video.play().catch(err => console.log('Ne može da se reprodukuje video:', err));
  }

  pauseVideo(event: Event) {
    const video = event.target as HTMLVideoElement;
    video.pause();
    video.currentTime = 0;
  }
}

