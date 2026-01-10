import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CommentsService } from '../../services/comments.service';
import { Comment } from '../../models/comment.model';
import { NgIf, NgFor } from '@angular/common';

@Component({
  selector: 'app-comment-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="comments">
      <h3>Comments ({{ total }})</h3>
      <div *ngIf="comments.length === 0">No comments yet.</div>
      <div *ngFor="let c of comments" class="comment">
        <div class="meta">{{ c.authorUsername }} • {{ c.createdAt | date:'short' }}</div>
        <div class="text">{{ c.text }}</div>
      </div>
      <button *ngIf="hasMore" (click)="loadMore()" class="load-more">Load more</button>
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
  @Input() videoId!: number;

  comments: Comment[] = [];
  page = 0;
  size = 10;
  total = 0;

  get hasMore() {
    return this.comments.length < this.total;
  }

  constructor(private commentsService: CommentsService) {}

  ngOnChanges(): void {
    this.resetAndLoad();
  }

  resetAndLoad() {
    this.page = 0;
    this.comments = [];
    this.total = 0;
    if (this.videoId) {
      this.loadPage();
    }
  }

  loadPage() {
    this.commentsService.getComments(this.videoId, this.page, this.size).subscribe(res => {
      this.comments = this.comments.concat(res.comments);
      this.total = res.total;
    });
  }

  loadMore() {
    this.page++;
    this.loadPage();
  }
}
