import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { Video } from '../../models/video.model';
import { Subscription } from 'rxjs';
import { CommentFormComponent } from '../comments/comment-form.component';
import { CommentListComponent } from '../comments/comment-list.component';
import { environment } from '../../env/environment';

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

  constructor(
    private route: ActivatedRoute,
    private videoService: VideoService
  ) {
    const nav = history.state;
      if (nav?.video) {
        this.video = nav.video;
      }
    }

  ngOnInit(): void {
    this.subscription.add(
      this.route.params.subscribe(params => {
        const id = +params['id'];
        if (!id) return;

        if (this.video) {
          this.incrementViews(id);
        }
        else {
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

    this.videoService.getVideoById(id).subscribe({
      next: (v) => {
        this.video = v;
        this.error = false;
        this.loading = false;

        this.incrementViews(id);
      },
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
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

  }

  incrementViews(id: number) {
    if (this.viewCountIncremented) return;
    this.viewCountIncremented = true;

    this.videoService.incrementViewCount(id).subscribe({
      next: (updated) => {
        if (this.video) {
          this.video.viewCount = updated.viewCount;
        }
      },
      error: () => console.log("View count failed")
    });
  }
}
