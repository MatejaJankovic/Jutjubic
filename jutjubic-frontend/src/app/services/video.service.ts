import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Video, VideoPageResponse } from '../models/video.model';

@Injectable({
  providedIn: 'root',
})
export class VideoService {
  private readonly API_URL = 'http://localhost:8080/api/videos';

  constructor(private http: HttpClient) {}

  getAllVideos(page: number = 0, size: number = 12): Observable<VideoPageResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<VideoPageResponse>(this.API_URL, { params });
  }

  getVideoById(id: number): Observable<Video> {
    return this.http.get<Video>(`${this.API_URL}/${id}`);
  }

  incrementViewCount(id: number): Observable<Video> {
    return this.http.post<Video>(`${this.API_URL}/${id}/view`, {});
  }

  searchVideos(query: string, page: number = 0, size: number = 12): Observable<VideoPageResponse> {
    const params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<VideoPageResponse>(`${this.API_URL}/search`, { params });
  }

  getTrendingVideos(page: number = 0, size: number = 12): Observable<VideoPageResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<VideoPageResponse>(`${this.API_URL}/trending`, { params });
  }

  formatDuration(seconds: number): string {
    if (!seconds) return '0:00';
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
  }

  formatViewCount(count: number): string {
    if (count >= 1000000) {
      return (count / 1000000).toFixed(1) + 'M';
    }
    if (count >= 1000) {
      return (count / 1000).toFixed(1) + 'K';
    }
    return count.toString();
  }

  formatRelativeTime(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffSecs = Math.floor(diffMs / 1000);
    const diffMins = Math.floor(diffSecs / 60);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);
    const diffWeeks = Math.floor(diffDays / 7);
    const diffMonths = Math.floor(diffDays / 30);
    const diffYears = Math.floor(diffDays / 365);

    if (diffYears > 0) return `pre ${diffYears} ${diffYears === 1 ? 'godinu' : 'godina'}`;
    if (diffMonths > 0) return `pre ${diffMonths} ${diffMonths === 1 ? 'mesec' : 'meseci'}`;
    if (diffWeeks > 0) return `pre ${diffWeeks} ${diffWeeks === 1 ? 'nedelju' : 'nedelja'}`;
    if (diffDays > 0) return `pre ${diffDays} ${diffDays === 1 ? 'dan' : 'dana'}`;
    if (diffHours > 0) return `pre ${diffHours} ${diffHours === 1 ? 'sat' : 'sati'}`;
    if (diffMins > 0) return `pre ${diffMins} ${diffMins === 1 ? 'minut' : 'minuta'}`;
    return 'upravo sada';
  }
}

