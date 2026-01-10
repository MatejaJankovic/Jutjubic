import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { Video } from '../../models/video.model';
import { Subscription } from 'rxjs';
import { CommentFormComponent } from '../comments/comment-form.component';
import { CommentListComponent } from '../comments/comment-list.component';

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
  loading = true;
  error = false;
  private subscription = new Subscription();

  constructor(
    private route: ActivatedRoute,
    private videoService: VideoService
  ) {}

  ngOnInit(): void {
    this.subscription.add(
      this.route.params.subscribe(params => {
        const id = +params['id'];
        if (id) {
          this.loadVideo(id);
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  loadVideo(id: number): void {
    this.loading = true;
    this.error = false;

    this.subscription.add(
      this.videoService.getVideoById(id).subscribe({
        next: (video) => {
          this.video = video;
          this.loading = false;
          // Increment view count
          this.videoService.incrementViewCount(id).subscribe();
        },
        error: () => {
          this.error = true;
          this.loading = false;
        }
      })
    );
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
  // Add a small getter so template can use videoId safely
  get videoId(): number | undefined {
    return this.video?.id;
  }

  // Called by comment form when a new comment is posted
  onCommentPosted(): void {
    if (this.video) {
      this.video.commentCount = (this.video.commentCount || 0) + 1;
    }
    // Optionally reload comments if your comment list does not auto-refresh
    // e.g. this.commentList?.reload();
  }
}
