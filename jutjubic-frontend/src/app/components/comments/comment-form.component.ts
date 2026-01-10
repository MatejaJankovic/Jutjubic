import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommentsService } from '../../services/comments.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-comment-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div *ngIf="authService.isAuthenticated(); else authRequired" class="comment-form">
      <textarea [(ngModel)]="text" rows="3" placeholder="Write a comment..." maxlength="2000"></textarea>
      <div class="actions">
        <button (click)="submit()" [disabled]="!text.trim() || submitting">Post comment</button>
      </div>
    </div>
    <ng-template #authRequired>
      <div class="auth-required">Authentication required</div>
    </ng-template>
  `,
  styles: [`
    .comment-form textarea { width: 100%; padding: 8px; border-radius: 6px; border: 1px solid #ccc; }
    .actions { margin-top: 0.5rem; text-align: right; }
    .auth-required { color: #c00; font-weight: 600; }
  `]
})
export class CommentFormComponent {
  @Input() videoId!: number;
  @Output() posted = new EventEmitter<void>();

  text = '';
  submitting = false;

  constructor(public authService: AuthService, private commentsService: CommentsService) {}

  submit() {
    if (!this.text.trim() || !this.videoId) return;
    this.submitting = true;
    this.commentsService.postComment(this.videoId, this.text.trim()).subscribe({
      next: () => {
        this.text = '';
        this.submitting = false;
        this.posted.emit();
      },
      error: () => {
        this.submitting = false;
        alert('Authentication required or error posting comment');
      },
    });
  }
}
