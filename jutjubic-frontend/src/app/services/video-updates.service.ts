import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface VideoUpdate {
  videoId: number;
  viewCount?: number;
  likeCount?: number;
}

/**
 * Service for broadcasting video updates across components.
 * Used to keep map markers in sync with video view counts.
 */
@Injectable({
  providedIn: 'root'
})
export class VideoUpdatesService {
  private videoUpdateSubject = new Subject<VideoUpdate>();

  // Observable that components can subscribe to
  videoUpdates$ = this.videoUpdateSubject.asObservable();

  /**
   * Broadcast a video update (e.g., view count increment)
   */
  notifyVideoUpdate(update: VideoUpdate): void {
    this.videoUpdateSubject.next(update);
  }
}

