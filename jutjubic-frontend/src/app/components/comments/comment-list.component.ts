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
        <h3>Comments ({{ state.total }})</h3>
        <div *ngIf="state.comments.length === 0">No comments yet.</div>
        <div *ngFor="let c of state.comments; trackBy: trackById" class="comment">
          <div class="meta"><a [routerLink]="['/users', c.authorUsername]">{{ c.authorUsername }}</a> • {{ c.createdAt | date:'short' }}</div>
          <div class="text">{{ c.text }}</div>
        </div>
        <button *ngIf="state.comments.length < state.total" (click)="loadMore()" class="load-more">Load more</button>
      </ng-container>
    </div>
  `,
  styles: [`
    .comments { margin-top: 1rem; }
    .comment { padding: 0.5rem 0; border-bottom: 1px solid #eaeaea; }
    .meta { font-size: 0.85rem; color: #666; }
    .text { margin-top: 0.25rem; }
    .load-more { margin-top: 0.5rem; }
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
          this.commentsService.getComments(videoId, page, this.size).pipe(
            map((res) => ({ page, res }))
          )
        ),
        scan((state, { page, res }) => {
          const merged = page === 0 ? res.comments : [...state.comments, ...res.comments];
          const unique = Array.from(new Map(merged.map(comment => [comment.id, comment])).values());
          return { comments: unique, total: res.total };
        }, { comments: [] as Comment[], total: 0 }),
        startWith({ comments: [] as Comment[], total: 0 })
      );
    }),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  readonly size = 10;

  constructor(private commentsService: CommentsService) {}

  loadMore() {

    this.loadMoreSubject.next();
  }

  trackById(_: number, comment: Comment) {
    return comment.id;}
}
