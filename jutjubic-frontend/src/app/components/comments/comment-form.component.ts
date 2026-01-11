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
    <div *ngIf="authService.isAuthenticated()" class="comment-form">
      <textarea
        [(ngModel)]="text"
        rows="3"
        placeholder="Napisi komentar..."
        maxlength="2000"></textarea>
      <div class="actions">
       <button class="post-button" (click)="submit()" [disabled]="!text.trim() || submitting">
          Postavi komentar
        </button>
      </div>
    </div>

  `,
  styles: [`
    .comment-form {
      padding: 16px;
      border-radius: 14px;
      border: 1px solid #2a2a2a;
      background: #181818;
    }
    .comment-form textarea {
      width: 100%;
      padding: 12px 14px;
      border-radius: 10px;
      border: 1px solid #2f2f2f;
      background: #121212;
      color: #f5f5f5;
      resize: vertical;
      line-height: 1.6;
      font-size: 14px;
      transition: border-color 0.2s ease, box-shadow 0.2s ease;
    }
    .comment-form textarea:focus {
      outline: none;
      border-color: #3ea6ff;
      box-shadow: 0 0 0 2px rgba(62, 166, 255, 0.2);
    }
    .comment-form textarea::placeholder { color: #8c8c8c; }
    .actions { margin-top: 12px; display: flex; justify-content: flex-end; }
    .post-button {
      padding: 8px 18px;
      border-radius: 999px;
      border: none;
      background: #3ea6ff;
      color: #0b0b0b;
      font-weight: 600;
      font-size: 13px;
      cursor: pointer;
      transition: transform 0.2s ease, opacity 0.2s ease;
    }
    .post-button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    .post-button:not(:disabled):hover { transform: translateY(-1px); }

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
