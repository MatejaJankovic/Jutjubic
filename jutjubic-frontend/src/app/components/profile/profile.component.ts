import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { UserService } from '../../services/user.service';
import { VideoService } from '../../services/video.service';
import { UserProfile } from '../../models/user-profile.model';
import { Video, VideoPageResponse } from '../../models/video.model';
import { environment } from '../../env/environment';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit, OnDestroy {
  username = '';
  profile: UserProfile | null = null;
  videos: Video[] = [];
  loading = true;
  videosLoading = false;
  error = '';
  environment = environment;

  // Pagination
  currentPage = 0;
  totalPages = 0;
  totalElements = 0;
  pageSize = 12;

  private subscription = new Subscription();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserService,
    private videoService: VideoService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.username = this.route.snapshot.paramMap.get('username') || '';

    // Listen for route parameter changes
    this.subscription.add(
      this.route.paramMap.subscribe(params => {
        const newUsername = params.get('username') || '';
        if (newUsername !== this.username) {
          this.username = newUsername;
          this.currentPage = 0;
          this.loadProfile();
        }
      })
    );

    if (!this.username) {
      this.error = 'Nevažeći korisnik';
      this.loading = false;
      return;
    }

    this.loadProfile();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  loadProfile(): void {
    this.loading = true;
    this.error = '';

    this.subscription.add(
      this.userService.getProfile(this.username).subscribe({
        next: (profile) => {
          this.profile = profile;
          this.loading = false;
          this.loadVideos();
          this.cdr.detectChanges();
        },
        error: () => {
          this.error = 'Korisnik nije pronađen';
          this.loading = false;
          this.cdr.detectChanges();
        }
      })
    );
  }

  loadVideos(): void {
    if (this.videosLoading) return;
    this.videosLoading = true;

    this.subscription.add(
      this.videoService.getVideosByUsername(this.username, this.currentPage, this.pageSize).subscribe({
        next: (response: VideoPageResponse) => {
          this.videos = response.videos;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;
          this.videosLoading = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.videosLoading = false;
          this.cdr.detectChanges();
        }
      })
    );
  }

  onVideoClick(video: Video): void {
    this.router.navigate(['/watch', video.id], { state: { video } });
  }

  // Pagination methods
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      this.currentPage = page;
      this.loadVideos();
      this.scrollToVideos();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadVideos();
      this.scrollToVideos();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadVideos();
      this.scrollToVideos();
    }
  }

  goToFirstPage(): void {
    if (this.currentPage !== 0) {
      this.currentPage = 0;
      this.loadVideos();
      this.scrollToVideos();
    }
  }

  goToLastPage(): void {
    const lastPage = this.totalPages - 1;
    if (this.currentPage !== lastPage) {
      this.currentPage = lastPage;
      this.loadVideos();
      this.scrollToVideos();
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

  scrollToVideos(): void {
    const videosSection = document.querySelector('.videos-section');
    if (videosSection) {
      videosSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  // Formatting helpers
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

  getProfileInitials(): string {
    if (!this.profile) return '';
    return (this.profile.firstName?.charAt(0) || '') + (this.profile.lastName?.charAt(0) || '');
  }

  playVideo(event: Event): void {
    const video = event.target as HTMLVideoElement;
    video.play().catch(err => console.log('Ne može da se reprodukuje video:', err));
  }

  pauseVideo(event: Event): void {
    const video = event.target as HTMLVideoElement;
    video.pause();
    video.currentTime = 0;
  }
}

