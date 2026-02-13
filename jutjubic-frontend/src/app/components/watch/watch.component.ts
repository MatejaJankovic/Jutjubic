
import { Component, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { Video, PremiereStatus } from '../../models/video.model';
import { Subscription, filter, distinctUntilChanged, map, of, switchMap, tap, interval } from 'rxjs';
import { CommentFormComponent } from '../comments/comment-form.component';
import { CommentListComponent } from '../comments/comment-list.component';
import { LiveChatComponent } from '../live-chat/live-chat.component';
import { environment } from '../../env/environment';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-watch',
  standalone: true,
  imports: [CommonModule, RouterLink, CommentFormComponent,
    CommentListComponent, LiveChatComponent],
  templateUrl: './watch.component.html',
  styleUrls: ['./watch.component.scss'],
})
export class WatchComponent implements OnInit, OnDestroy {
  @ViewChild('videoPlayer') videoPlayer!: ElementRef<HTMLVideoElement>;

  video: Video | null = null;
  error = false;
  loading = false;
  environment = environment;
  private viewCountIncremented = false;
  private subscription = new Subscription();
  commentRefreshToken = 0;

  // Premiere state
  premiereStatus: PremiereStatus | null = null;
  isPremiereMode = false;
  countdownDisplay = '';
  private premierePollingSubscription?: Subscription;
  private countdownInterval?: any;

  // Video playback state
  isVideoReady = false;
  showClickToPlay = false;
  isSeekingDisabled = false;
  private syncInterval?: any;
  private lastSyncedTime = 0;

  constructor(
    private route: ActivatedRoute,
    private videoService: VideoService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {
    const nav = history.state;
    if (nav?.video) {
      this.video = nav.video;
    }
  }


  ngOnInit(): void {
    this.subscription.add(
      this.route.paramMap.pipe(
        map(params => Number(params.get('id'))),
        filter((id): id is number => Number.isFinite(id) && id > 0),
        distinctUntilChanged(),
        tap(() => {
          this.error = false;
          this.loading = true;
          this.viewCountIncremented = false;
        }),
        switchMap((id) => {
          if (this.video?.id === id) {
            return of(this.video);
          }
          return this.videoService.getVideoById(id);
        })
      ).subscribe({
        next: (video) => {
          if (!video) return;
          this.video = video;
          this.loading = false;
          this.cdr.detectChanges();

          // Check if this is a premiere video (has premiereScheduledAt)
          if (video.premiereScheduledAt) {
            this.isPremiereMode = true;
            this.initPremiere(video.id);
          } else {
            this.isPremiereMode = false;
            this.incrementViews(video.id);
          }
        },
        error: (err) => {
          console.error('Error loading video:', err);
          this.error = true;
          this.loading = false;
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
    this.stopPremierePolling();
    this.stopCountdown();
    this.stopVideoSync();

    // Leave premiere if we were watching one
    if (this.video?.id && this.isPremiereMode) {
      this.videoService.leavePremiere(this.video.id).subscribe();
    }
  }

  // ==================== PREMIERE METHODS ====================

  private initPremiere(videoId: number): void {
    // Join the premiere for viewer counting
    this.videoService.joinPremiere(videoId).subscribe();

    // Get initial premiere status
    this.videoService.getPremiereStatus(videoId).subscribe({
      next: (status) => {
        this.premiereStatus = status;
        this.handlePremiereStatus(status);
      },
      error: (err) => {
        console.error('Error getting premiere status:', err);
        // Fall back to normal video mode
        this.isPremiereMode = false;
        this.incrementViews(videoId);
      }
    });
  }

  private handlePremiereStatus(status: PremiereStatus): void {
    this.premiereStatus = status;

    switch (status.status) {
      case 'SCHEDULED':
        this.isSeekingDisabled = true;
        this.startCountdown(status.secondsUntilStart);
        this.startPremierePolling();
        break;

      case 'LIVE':
        this.stopCountdown();
        this.isSeekingDisabled = true; // Disable seeking during live premiere
        this.startPremierePolling();
        this.startLiveVideo(status.startOffset); // Start video at correct offset
        this.incrementViews(status.videoId);
        break;

      case 'ENDED':
        this.stopCountdown();
        this.stopPremierePolling();
        this.stopVideoSync();
        this.isSeekingDisabled = false; // Allow seeking after premiere ends
        this.isPremiereMode = false;
        this.incrementViews(status.videoId);
        break;
    }

    this.cdr.detectChanges();
  }

  private startLiveVideo(startOffset: number): void {
    this.lastSyncedTime = startOffset;
    this.showClickToPlay = true;

    // Wait for video element to be ready
    setTimeout(() => {
      const videoEl = this.videoPlayer?.nativeElement;
      if (videoEl) {
        // Set video to correct offset based on server time
        videoEl.currentTime = startOffset;
        this.startVideoSync(); // Start continuous syncing
        this.cdr.detectChanges();
      }
    }, 500);
  }

  private startVideoSync(): void {
    this.stopVideoSync();

    // Only update lastSyncedTime locally based on video's current playback
    // This avoids server calls that cause video jitter
    this.syncInterval = setInterval(() => {
      const videoEl = this.videoPlayer?.nativeElement;
      if (videoEl && !videoEl.paused && this.premiereStatus?.status === 'LIVE') {
        // Just track the current playback position for seeking prevention
        this.lastSyncedTime = videoEl.currentTime;
      }
    }, 1000);
  }

  private stopVideoSync(): void {
    if (this.syncInterval) {
      clearInterval(this.syncInterval);
      this.syncInterval = undefined;
    }
  }

  private startPremierePolling(): void {
    this.stopPremierePolling();

    if (!this.video?.id) return;

    // Poll every 10 seconds for status updates
    // This is mainly to detect SCHEDULED -> LIVE and LIVE -> ENDED transitions
    this.premierePollingSubscription = interval(10000).subscribe(() => {
      if (this.video?.id) {
        this.videoService.getPremiereStatus(this.video.id).subscribe({
          next: (status) => {
            const previousStatus = this.premiereStatus?.status;
            this.premiereStatus = status;

            // Only handle status transitions, don't interrupt video playback
            if (previousStatus !== status.status) {
              this.handlePremiereStatus(status);
            }
          }
        });
      }
    });
  }

  private stopPremierePolling(): void {
    if (this.premierePollingSubscription) {
      this.premierePollingSubscription.unsubscribe();
      this.premierePollingSubscription = undefined;
    }
  }

  private startCountdown(secondsUntilStart: number): void {
    this.stopCountdown();

    let remaining = secondsUntilStart;
    this.updateCountdownDisplay(remaining);

    this.countdownInterval = setInterval(() => {
      remaining--;
      if (remaining <= 0) {
        this.stopCountdown();
        // Trigger status refresh
        if (this.video?.id) {
          this.videoService.getPremiereStatus(this.video.id).subscribe({
            next: (status) => this.handlePremiereStatus(status)
          });
        }
      } else {
        this.updateCountdownDisplay(remaining);
      }
    }, 1000);
  }

  private stopCountdown(): void {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
      this.countdownInterval = undefined;
    }
  }

  private updateCountdownDisplay(seconds: number): void {
    this.countdownDisplay = this.videoService.formatCountdown(seconds);
    this.cdr.detectChanges();
  }

  // ==================== END PREMIERE METHODS ====================

  formatViewCount(count: number): string {
    return this.videoService.formatViewCount(count);
  }

  formatRelativeTime(date: string): string {
    return this.videoService.formatRelativeTime(date);
  }

  getChannelInitials(): string {
    if (!this.video) return '';
    return (this.video.userFirstName?.charAt(0) || '') + (this.video.userLastName?.charAt(0) || '');
  }

  onVideoReady(): void {
    // Video metadata is loaded and ready
    this.isVideoReady = true;
    if (this.isPremiereMode && this.premiereStatus?.status === 'LIVE') {
      this.startLiveVideo(this.premiereStatus.startOffset);
    }
  }

  onVideoClick(): void {
    // Handle user interaction to enable sound and play
    const videoEl = this.videoPlayer?.nativeElement;
    if (videoEl) {
      videoEl.muted = false; // Unmute after user interaction
      // Set to current synced time for live premiere
      if (this.isPremiereMode && this.premiereStatus?.status === 'LIVE') {
        videoEl.currentTime = this.lastSyncedTime;
      }
      videoEl.play().then(() => {
        this.showClickToPlay = false; // Hide the overlay once playing
        this.cdr.detectChanges();
      }).catch(err => {
        console.log('Play failed:', err);
      });
    }
  }

  onSeeking(): void {
    // Prevent seeking during live premiere
    if (this.isSeekingDisabled && this.premiereStatus?.status === 'LIVE') {
      const videoEl = this.videoPlayer?.nativeElement;
      if (videoEl) {
        // Snap back to the synced live time
        const diff = Math.abs(videoEl.currentTime - this.lastSyncedTime);
        if (diff > 1) {
          videoEl.currentTime = this.lastSyncedTime;
        }
      }
    }
  }

  get videoId(): number | undefined {
    return this.video?.id;
  }

  onCommentPosted(): void {
    if (this.video) {
      this.video.commentCount = (this.video.commentCount || 0) + 1;
    }
    this.commentRefreshToken++;
  }

  incrementViews(id: number) {
    if (this.viewCountIncremented) return;
    this.viewCountIncremented = true;

    // Optimistic UI update - odmah inkrementiraj broj pregleda
    if (this.video) {
      this.video.viewCount = this.video.viewCount + 1;
    }

    this.videoService.incrementViewCount(id).subscribe({
      next: (updated) => {
        // Ažuriraj sa tačnim brojem sa servera
        if (this.video) {
          this.video.viewCount = updated.viewCount;
        }
      },
      error: () => {
        console.log("View count failed");
        // Vrati na prethodno stanje ako je došlo do greške
        if (this.video) {
          this.video.viewCount = this.video.viewCount - 1;
        }
      }
    });
  }
  handleLikeClick(): void {
    if (!this.authService.isAuthenticated()) {
      alert('You must log in to use this feature.');
      return;
    }

    if (!this.video) return;

    // Optimistic UI update - odmah ažuriraj UI pre nego što stigne odgovor sa servera
    const wasLiked = this.video.isLikedByCurrentUser;
    const previousLikeCount = this.video.likeCount;

    // Odmah ažuriraj UI
    this.video.isLikedByCurrentUser = !wasLiked;
    this.video.likeCount = wasLiked ? previousLikeCount - 1 : previousLikeCount + 1;

    this.videoService.toggleLike(this.video.id).subscribe({
      next: (updatedVideo) => {
        // Ažuriraj sa stvarnim podacima sa servera
        if (this.video) {
          this.video.likeCount = updatedVideo.likeCount;
          this.video.isLikedByCurrentUser = updatedVideo.isLikedByCurrentUser;
        }
      },
      error: (err) => {
        console.error('Failed to toggle like:', err);
        // Vrati na prethodno stanje ako je došlo do greške
        if (this.video) {
          this.video.isLikedByCurrentUser = wasLiked;
          this.video.likeCount = previousLikeCount;
        }
        alert('Failed to like video. Please try again.');
      }
    });
  }
}
