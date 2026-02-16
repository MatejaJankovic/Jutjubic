import { Component, OnInit, OnDestroy, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, Subscription, debounceTime, distinctUntilChanged, startWith, switchMap, catchError, of, merge } from 'rxjs';
import { WatchPartyService } from '../../services/watch-party.service';
import { VideoService } from '../../services/video.service';
import { Video, VideoPageResponse } from '../../models/video.model';
import { environment } from '../../env/environment';

@Component({
  selector: 'app-watch-party',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './watch-party.component.html',
  styleUrls: ['./watch-party.component.scss']
})
export class WatchPartyComponent implements OnInit, OnDestroy {
  videos: Video[] = [];
  loading = true;
  creatingParty = false;
  error = '';
  searchQuery = '';
  currentPage = 0;
  totalPages = 0;
  pageSize = 12;
  environment = environment;

  private searchSubject = new Subject<string>();
  private subscriptions = new Subscription();
  private initialLoad$ = new Subject<void>();

  constructor(
    private watchPartyService: WatchPartyService,
    private videoService: VideoService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    // Combine initial load trigger with search subject
    this.subscriptions.add(
      merge(
        this.initialLoad$.pipe(startWith(undefined)), // Emit immediately on init
        this.searchSubject.pipe(
          debounceTime(300),
          distinctUntilChanged()
        )
      ).pipe(
        switchMap((query) => {
          // If it's the initial load (undefined) or empty string, use empty search
          const searchTerm = typeof query === 'string' ? query : '';
          this.searchQuery = searchTerm;
          this.loading = true;
          this.error = '';
          this.cdr.detectChanges();

          const request = searchTerm
            ? this.videoService.searchVideos(searchTerm, this.currentPage, this.pageSize)
            : this.videoService.getAllVideos(this.currentPage, this.pageSize);

          return request.pipe(
            catchError(err => {
              console.error('Failed to load videos:', err);
              this.ngZone.run(() => {
                this.error = 'Failed to load videos';
                this.loading = false;
                this.cdr.detectChanges();
              });
              return of(null);
            })
          );
        })
      ).subscribe(response => {
        this.ngZone.run(() => {
          if (response) {
            console.log('Videos loaded:', response);
            this.videos = response.videos;
            this.totalPages = response.totalPages;
          }
          this.loading = false;
          this.cdr.detectChanges();
        });
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  onSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.currentPage = 0; // Reset to first page on search
    this.searchSubject.next(input.value);
  }

  loadVideos(): void {
    this.loading = true;
    this.error = '';
    this.cdr.detectChanges();

    const request = this.searchQuery
      ? this.videoService.searchVideos(this.searchQuery, this.currentPage, this.pageSize)
      : this.videoService.getAllVideos(this.currentPage, this.pageSize);

    this.subscriptions.add(
      request.subscribe({
        next: (response: VideoPageResponse) => {
          this.ngZone.run(() => {
            console.log('Videos loaded:', response);
            this.videos = response.videos;
            this.totalPages = response.totalPages;
            this.loading = false;
            this.cdr.detectChanges();
          });
        },
        error: (err) => {
          this.ngZone.run(() => {
            console.error('Failed to load videos:', err);
            this.loading = false;
            this.error = 'Failed to load videos';
            this.cdr.detectChanges();
          });
        }
      })
    );
  }

  createWatchParty(video: Video): void {
    this.creatingParty = true;
    this.error = '';
    this.cdr.detectChanges();

    this.subscriptions.add(
      this.watchPartyService.createRoom(video.id, false).subscribe({
        next: (room) => {
          console.log('Watch party created:', room);
          this.creatingParty = false;
          this.cdr.detectChanges();
          this.router.navigate(['/watch-party/room', room.id]);
        },
        error: (err) => {
          this.ngZone.run(() => {
            this.creatingParty = false;
            this.error = 'Failed to create Watch Party. Please try again.';
            this.cdr.detectChanges();
          });
          console.error('Error creating watch party:', err);
        }
      })
    );
  }

  // Pagination methods
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

  playVideo(event: Event): void {
    const video = event.target as HTMLVideoElement;
    video.play().catch(err => console.log('Cannot play video:', err));
  }

  pauseVideo(event: Event): void {
    const video = event.target as HTMLVideoElement;
    video.pause();
    video.currentTime = 0;
  }
}

