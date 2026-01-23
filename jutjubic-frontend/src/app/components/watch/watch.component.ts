
import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { Video } from '../../models/video.model';
import { Subscription, filter, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';
import { CommentFormComponent } from '../comments/comment-form.component';
import { CommentListComponent } from '../comments/comment-list.component';
import { environment } from '../../env/environment';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-watch',
  standalone: true,
  imports: [CommonModule, RouterLink, CommentFormComponent,
    CommentListComponent],
  templateUrl: './watch.component.html',
  styleUrls: ['./watch.component.scss'],
})
export class WatchComponent implements OnInit, OnDestroy {
  video: Video | null = null;
  error = false;
  loading = false;
  environment = environment;
  private viewCountIncremented = false;
  private subscription = new Subscription();
  commentRefreshToken = 0;
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
          this.incrementViews(video.id);
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
