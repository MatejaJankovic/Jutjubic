import { Component, OnInit, OnDestroy, ViewChild, ElementRef, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { filter, takeWhile } from 'rxjs/operators';
import { WatchPartyService } from '../../services/watch-party.service';
import { VideoService } from '../../services/video.service';
import { AuthService } from '../../services/auth.service';
import { WatchParty, WatchPartyEvent } from '../../models/watch-party.model';
import { Video } from '../../models/video.model';
import { environment } from '../../env/environment';
import { RecommendedVideosComponent } from '../recommended-videos/recommended-videos.component';

@Component({
  selector: 'app-watch-party-room',
  standalone: true,
  imports: [CommonModule, RecommendedVideosComponent],
  templateUrl: './watch-party-room.component.html',
  styleUrls: ['./watch-party-room.component.scss']
})
export class WatchPartyRoomComponent implements OnInit, OnDestroy {
  @ViewChild('videoPlayer') videoPlayer!: ElementRef<HTMLVideoElement>;

  room: WatchParty | null = null;
  video: Video | null = null;
  loading = true;
  error = '';
  connected = false;
  roomId: number | null = null;
  inviteLink = '';
  participantCount = 1;
  status: 'waiting' | 'ready' | 'playing' = 'waiting';
  isCreator = false;
  startEventSent = false;
  copySuccess = false;
  environment = environment;
  switchingVideo = false;
  videoSwitchError = '';

  private subscriptions = new Subscription();
  private currentUserId: number | null = null;
  private pollingActive = true;

  constructor(
    private watchPartyService: WatchPartyService,
    private videoService: VideoService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    // Get current user
    const currentUser = this.authService.getCurrentUser();
    this.currentUserId = currentUser?.id || null;
    console.log('WatchPartyRoom - Current user ID:', this.currentUserId);

    // Get roomId from route parameter
    const roomIdParam = this.route.snapshot.paramMap.get('roomId');
    console.log('WatchPartyRoom - Route param roomId:', roomIdParam);
    this.roomId = roomIdParam ? parseInt(roomIdParam, 10) : null;

    if (!this.roomId || isNaN(this.roomId)) {
      console.error('WatchPartyRoom - Invalid room ID:', roomIdParam);
      this.error = 'Invalid room ID';
      this.loading = false;
      this.cdr.detectChanges();
      return;
    }

    console.log('WatchPartyRoom - Loading room with ID:', this.roomId);
    this.loadRoom();
  }

  ngOnDestroy(): void {
    this.pollingActive = false;
    this.watchPartyService.disconnect();
    this.subscriptions.unsubscribe();
  }

  private loadRoom(): void {
    this.loading = true;
    this.error = '';
    this.cdr.detectChanges();

    this.subscriptions.add(
      this.watchPartyService.getRoom(this.roomId!).subscribe({
        next: (room: WatchParty) => {
          this.ngZone.run(() => {
            console.log('WatchPartyRoom - Room loaded:', room);
            this.room = room;
            this.isCreator = room.creatorId === this.currentUserId;
            this.participantCount = room.participantIds?.length || 1;
            this.generateInviteLink(room.inviteCode);
            this.updateStatus();
            this.cdr.detectChanges();

            // Load the current video (use currentVideoId if set, otherwise fall back to videoId)
            const videoToLoad = room.currentVideoId || room.videoId;
            this.loadVideo(videoToLoad);

            // Connect to WebSocket
            this.connectWebSocket();

            // Start backup polling for participant count
            this.startBackupPolling();
          });
        },
        error: (err) => {
          this.ngZone.run(() => {
            this.loading = false;
            this.error = 'Failed to load room. Room not found or access denied.';
            this.cdr.detectChanges();
          });
          console.error('Error loading room:', err);
        }
      })
    );
  }

  private loadVideo(videoId: number): void {
    this.subscriptions.add(
      this.videoService.getVideoById(videoId).subscribe({
        next: (video: Video) => {
          this.ngZone.run(() => {
            this.video = video;
            this.loading = false;
            this.cdr.detectChanges();
          });
        },
        error: (err) => {
          this.ngZone.run(() => {
            this.loading = false;
            this.error = 'Failed to load video.';
            this.cdr.detectChanges();
          });
          console.error('Error loading video:', err);
        }
      })
    );
  }

  private connectWebSocket(): void {
    // Subscribe to connection status
    this.subscriptions.add(
      this.watchPartyService.connected$.subscribe(connected => {
        this.ngZone.run(() => {
          this.connected = connected;
          console.log('WebSocket connected:', connected);
          this.cdr.detectChanges();
        });
      })
    );

    // Connect and subscribe to events
    this.subscriptions.add(
      this.watchPartyService.connect(this.roomId!).pipe(
        filter((event): event is WatchPartyEvent => event !== null)
      ).subscribe(event => {
        this.ngZone.run(() => {
          this.handleWatchPartyEvent(event);
        });
      })
    );
  }

  private handleWatchPartyEvent(event: WatchPartyEvent): void {
    console.log('Watch Party event received:', event);

    switch (event.type) {
      case 'USER_JOINED':
        // Use participantCount from event if available, otherwise increment
        if (event.participantCount !== undefined) {
          this.participantCount = event.participantCount;
        } else {
          this.participantCount++;
        }
        console.log(`User joined. Participants: ${this.participantCount}`);
        this.updateStatus();
        this.cdr.detectChanges();
        break;

      case 'START':
        console.log('START event received, playing video');
        this.status = 'playing';
        this.cdr.detectChanges();
        this.playVideo();
        break;

      case 'VIDEO_SWITCHED':
        console.log('VIDEO_SWITCHED event received, switching to video:', event.videoId);
        this.handleVideoSwitched(event.videoId);
        break;
    }
  }

  /**
   * Handle VIDEO_SWITCHED event - load new video and update UI
   */
  private handleVideoSwitched(newVideoId: number): void {
    console.log('Switching to new video:', newVideoId);

    // Update room's currentVideoId
    if (this.room) {
      this.room.currentVideoId = newVideoId;
    }

    // Load the new video
    this.subscriptions.add(
      this.videoService.getVideoById(newVideoId).subscribe({
        next: (video: Video) => {
          this.ngZone.run(() => {
            console.log('New video loaded:', video);
            this.video = video;

            // Reset and play the video
            setTimeout(() => {
              const videoEl = this.videoPlayer?.nativeElement;
              if (videoEl) {
                videoEl.load(); // Reload the video source
                if (this.status === 'playing') {
                  videoEl.play().catch(err => {
                    console.log('Autoplay blocked after switch:', err);
                  });
                }
              }
            }, 100);

            this.cdr.detectChanges();
          });
        },
        error: (err) => {
          console.error('Error loading new video:', err);
          this.ngZone.run(() => {
            this.videoSwitchError = 'Failed to load the new video';
            this.cdr.detectChanges();
            setTimeout(() => {
              this.videoSwitchError = '';
              this.cdr.detectChanges();
            }, 3000);
          });
        }
      })
    );
  }

  /**
   * Called when owner selects a video from recommended list
   */
  onVideoSelected(selectedVideo: Video): void {
    if (!this.isCreator || !this.roomId || this.switchingVideo) {
      console.warn('Cannot switch video: not owner or already switching');
      return;
    }

    console.log('Owner selected video:', selectedVideo);
    this.switchingVideo = true;
    this.videoSwitchError = '';
    this.cdr.detectChanges();

    this.subscriptions.add(
      this.watchPartyService.switchVideo(this.roomId, selectedVideo.id).subscribe({
        next: (updatedRoom) => {
          this.ngZone.run(() => {
            console.log('Video switch successful:', updatedRoom);
            this.room = updatedRoom;
            this.switchingVideo = false;
            // Note: The actual video update will come from the WebSocket VIDEO_SWITCHED event
            this.cdr.detectChanges();
          });
        },
        error: (err) => {
          this.ngZone.run(() => {
            console.error('Error switching video:', err);
            this.switchingVideo = false;
            this.videoSwitchError = err.error || 'Failed to switch video. Only the room owner can switch videos.';
            this.cdr.detectChanges();
            setTimeout(() => {
              this.videoSwitchError = '';
              this.cdr.detectChanges();
            }, 5000);
          });
        }
      })
    );
  }

  private updateStatus(): void {
    if (this.participantCount >= 2 && this.status === 'waiting') {
      this.status = 'ready';
      this.checkAndSendStart();
    }
  }

  /**
   * Backup polling to ensure participant count is synced
   * Polls every 3 seconds for first 30 seconds, then stops
   */
  private startBackupPolling(): void {
    let elapsedSeconds = 0;
    const maxSeconds = 30;
    const pollIntervalMs = 3000;

    this.subscriptions.add(
      interval(pollIntervalMs).pipe(
        takeWhile(() => this.pollingActive && elapsedSeconds < maxSeconds && this.participantCount < 2)
      ).subscribe(() => {
        elapsedSeconds += pollIntervalMs / 1000;
        console.log('Backup polling for room state...');

        this.watchPartyService.getRoom(this.roomId!).subscribe({
          next: (room) => {
            this.ngZone.run(() => {
              const newCount = room.participantIds?.length || 1;
              if (newCount !== this.participantCount) {
                console.log(`Participant count updated via polling: ${this.participantCount} -> ${newCount}`);
                this.participantCount = newCount;
                this.updateStatus();
                this.cdr.detectChanges();
              }
            });
          },
          error: (err) => console.error('Polling error:', err)
        });
      })
    );
  }

  private checkAndSendStart(): void {
    // Only creator sends START, and only once
    if (this.isCreator && !this.startEventSent && this.participantCount >= 2) {
      console.log('Creator sending START event');
      this.startEventSent = true;

      // Small delay to ensure both clients are ready
      setTimeout(() => {
        this.watchPartyService.sendStart(this.roomId!, this.room!.videoId);
      }, 500);
    }
  }

  private playVideo(): void {
    setTimeout(() => {
      const videoEl = this.videoPlayer?.nativeElement;
      if (videoEl) {
        videoEl.currentTime = 0;
        videoEl.play().catch(err => {
          console.log('Autoplay blocked, user needs to click play:', err);
        });
      }
    }, 100);
  }

  private generateInviteLink(inviteCode: string): void {
    this.inviteLink = `${window.location.origin}/watch-party/join/${inviteCode}`;
  }

  copyInviteLink(): void {
    navigator.clipboard.writeText(this.inviteLink).then(() => {
      this.ngZone.run(() => {
        this.copySuccess = true;
        this.cdr.detectChanges();
        setTimeout(() => {
          this.copySuccess = false;
          this.cdr.detectChanges();
        }, 2000);
      });
    }).catch(err => {
      console.error('Failed to copy invite link:', err);
    });
  }

  getStatusText(): string {
    switch (this.status) {
      case 'waiting':
        return 'Čekanje drugog korisnika...';
      case 'ready':
        return 'Spremno za početak!';
      case 'playing':
        return 'Reprodukuje se';
      default:
        return '';
    }
  }

  goBack(): void {
    this.router.navigate(['/watch-party']);
  }

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
}

