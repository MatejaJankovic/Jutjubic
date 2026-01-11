import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CommentsService } from '../../services/comments.service';
import { Comment } from '../../models/comment.model';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, Subject, combineLatest, distinctUntilChanged, filter, map, merge, of, scan, shareReplay, startWith, switchMap } from 'rxjs';

@Component({
  selector: 'app-comment-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="comments">
      <ng-container *ngIf="state$ | async as state">

          <div class="comments-header">
            <h3>Komentari <span class="count">({{ state.total }})</span></h3>
          </div>

          <div *ngIf="state.loading" class="comments-skeleton">
            <div *ngFor="let _ of skeletonItems" class="comment-card skeleton">
              <div class="avatar skeleton-avatar"></div>
              <div class="comment-body">
                <div class="skeleton-line short"></div>
                <div class="skeleton-line"></div>
                <div class="skeleton-line half"></div>
              </div>
            </div>
          </div>

          <div *ngIf="!state.loading && state.comments.length === 0" class="comments-empty">
            I dalje nema komentara, budi prvi koji ce ostaviti komentar!
          </div>

          <div *ngFor="let c of state.comments; trackBy: trackByCommentId" class="comment-card">
            <div class="avatar">{{ getInitials(c) }}</div>
            <div class="comment-body">
              <div class="comment-meta">
                <a class="username" [routerLink]="['/users', getAuthorUsername(c)]">{{ getAuthorName(c) }}</a>
                <span class="dot">•</span>
                <span class="time">{{ formatRelativeTime(c.createdAt) }}</span>
              </div>
              <p class="comment-text">{{ c.text }}</p>
<!--              <div class="comment-actions">-->
<!--                <button type="button" class="comment-action">Odgovori</button>-->
<!--                <button type="button" class="comment-action">Svidja mi se</button>-->
<!--              </div>-->
            </div>
          </div>

          <button *ngIf="state.comments.length < state.total" (click)="loadMore()" class="load-more">
            Load more
          </button>
      </ng-container>
    </div>
  `,
  styles: [`
    :host { display: block; }
    .comments { display: flex; flex-direction: column; gap: 16px; }
    .comments-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: #f5f5f5; }
    .comments-header .count { color: #9a9a9a; font-weight: 500; }
    .comments-empty { padding: 20px; border-radius: 12px; border: 1px dashed #2a2a2a; color: #9a9a9a; background: #161616; text-align: center; }

    .comment-card {
      display: flex;
      gap: 16px;
      padding: 16px;
      border-radius: 14px;
      border: 1px solid #2a2a2a;
      background: #181818;
      transition: border-color 0.2s ease, background 0.2s ease;
    }

    .comment-card:hover { border-color: #3a3a3a; background: #1d1d1d; }

    .avatar {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      background: linear-gradient(135deg, #3a3a3a, #242424);
      color: #ffffff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      font-size: 14px;
      text-transform: uppercase;
      flex-shrink: 0;
    }

    .comment-body { flex: 1; min-width: 0; }
    .comment-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .username { color: #f1f1f1; font-weight: 600; text-decoration: none; }
    .username:hover { color: #3ea6ff; }
    .time { color: #9a9a9a; font-size: 12px; }
    .dot { color: #555; }
    .comment-text { margin: 8px 0 12px; color: #e0e0e0; line-height: 1.6; white-space: pre-wrap; }
    .comment-actions { display: flex; gap: 8px; }
    .comment-action {
      background: transparent;
      border: 1px solid #333;
      color: #cfcfcf;
      padding: 4px 12px;
      border-radius: 999px;
      font-size: 12px;
      cursor: pointer;
      transition: all 0.2s ease;
    }
    .comment-action:hover { background: #2a2a2a; color: #ffffff; border-color: #3a3a3a; }
    .comment-action:focus-visible { outline: 2px solid #3ea6ff; outline-offset: 2px; }

    .load-more {
      align-self: center;
      padding: 8px 16px;
      border-radius: 999px;
      border: 1px solid #333;
      background: #1a1a1a;
      color: #f5f5f5;
      cursor: pointer;
      transition: all 0.2s ease;
    }
    .load-more:hover { background: #242424; border-color: #3a3a3a; }

    .comments-skeleton { display: flex; flex-direction: column; gap: 16px; }
    .comment-card.skeleton { border-color: #232323; background: #151515; }
    .skeleton-avatar { background: #222; }
    .skeleton-line {
      height: 10px;
      border-radius: 999px;
      background: linear-gradient(90deg, #222 0%, #2c2c2c 50%, #222 100%);
      background-size: 200% 100%;
      animation: shimmer 1.5s infinite;
      margin-top: 8px;
    }
    .skeleton-line.short { width: 40%; margin-top: 0; }
    .skeleton-line.half { width: 65%; }

    @keyframes shimmer {
      0% { background-position: -200% 0; }
      100% { background-position: 200% 0; }
    }

    @media (max-width: 640px) {
      .comment-card { flex-direction: column; }
      .avatar { width: 36px; height: 36px; font-size: 12px; }
    }
  `]
})
export class CommentListComponent {
  private readonly videoIdSubject = new BehaviorSubject<number | null>(null);
  private readonly refreshSubject = new Subject<void>();
  private readonly loadMoreSubject = new Subject<void>();
  private refreshTokenValue = 0;

  @Input() set videoId(value: number) {
    if (value) {
      this.videoIdSubject.next(value);
    }
  }

  @Input() set refreshToken(_: number) {
    if (_ !== this.refreshTokenValue) {
      this.refreshTokenValue = _;
      this.refreshSubject.next();
    }
  }

  readonly state$ = combineLatest([
    this.videoIdSubject.pipe(
      filter((id): id is number => id !== null),
      distinctUntilChanged()
    ),
    this.refreshSubject.pipe(startWith(undefined))
  ]).pipe(
    switchMap(([videoId]) => {
      return merge(of(0), this.loadMoreSubject.pipe(map(() => 1))).pipe(
        scan((page, increment) => page + increment, 0),
        switchMap((page) =>
            merge(
              of({ type: 'loading' as const, page }),
              this.commentsService.getComments(videoId, page, this.size).pipe(
                map((res) => ({ type: 'loaded' as const, page, res }))
              )
          )
        ),
        scan((state, event) => {
          if (event.type === 'loading') {
            return { ...state, loading: true };
          }
          const merged = event.page === 0 ? event.res.comments : [...state.comments, ...event.res.comments];
          const unique = Array.from(new Map(merged.map((comment) => [comment.id, comment])).values());
          return { comments: unique, total: event.res.total, loading: false };
        }, { comments: [] as Comment[], total: 0, loading: true }),
        startWith({ comments: [] as Comment[], total: 0, loading: true })
      );
    }),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  readonly size = 10;
  readonly skeletonItems = Array.from({ length: 3 });

  constructor(private commentsService: CommentsService) {}

  loadMore() {

    this.loadMoreSubject.next();
  }

  trackByCommentId(_: number, comment: Comment) {
    return comment.id;
  }

  getAuthorName(comment: Comment) {
    return comment.authorUsername || comment.username || comment.authorName || 'User';
  }

  getAuthorUsername(comment: Comment) {
    return comment.authorUsername || comment.username || 'user';
  }

  getInitials(comment: Comment) {
    const name = this.getAuthorName(comment).trim();
    if (!name) return '?';
    const parts = name.split(' ').filter(Boolean);
    const first = parts[0]?.charAt(0) ?? '';
    const last = parts.length > 1 ? parts[parts.length - 1].charAt(0) : '';
    return `${first}${last}`.toUpperCase();
  }

  formatRelativeTime(dateString: string) {
    const date = new Date(dateString);
    if (Number.isNaN(date.getTime())) return '';
    const diffMs = Date.now() - date.getTime();
    const diffSeconds = Math.floor(diffMs / 1000);
    if (diffSeconds < 60) return 'upravo sada';
    const diffMinutes = Math.floor(diffSeconds / 60);
    if (diffMinutes < 60) return `pre ${diffMinutes} min`;
    const diffHours = Math.floor(diffMinutes / 60);
    if (diffHours < 24) return `pre ${diffHours} h`;
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays < 7) return `pre ${diffDays} dana`;
    return date.toLocaleDateString();
  }
}
