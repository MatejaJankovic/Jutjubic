import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { Video, VideoPageResponse } from '../../models/video.model';
import { Subscription } from 'rxjs';
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
  hasNext = false;
  searchQuery = '';
  environment = environment;
  activeVideo: Video | null = null;
  private subscription = new Subscription();

  constructor(
    private videoService: VideoService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.subscription.add(
      this.route.queryParams.subscribe(params => {
        this.searchQuery = params['search'] || '';
        this.currentPage = 0;
        this.videos = [];
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

    const request = this.searchQuery
      ? this.videoService.searchVideos(this.searchQuery, this.currentPage, 12)
      : this.videoService.getAllVideos(this.currentPage, 12);

    this.subscription.add(
      request.subscribe({
        next: (response: VideoPageResponse) => {
          this.videos = [...this.videos, ...response.videos];
          this.totalPages = response.totalPages;
          this.hasNext = response.hasNext;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
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

//   onVideoClick(video: Video): void {
//     this.router.navigate(['/watch', video.id]);
//   }

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
