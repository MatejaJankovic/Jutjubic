import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Video, VideoPageResponse, PremiereStatus, PopularVideosResponse } from '../models/video.model';

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

  toggleLike(id: number): Observable<Video> {
    return this.http.post<Video>(`${this.API_URL}/${id}/like`, {});
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

  getRecommendedVideos(excludeVideoId?: number, page: number = 0, size: number = 10): Observable<VideoPageResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (excludeVideoId) {
      params = params.set('excludeVideoId', excludeVideoId.toString());
    }

    return this.http.get<VideoPageResponse>(`${this.API_URL}/recommended`, { params });
  }

  getVideosByUsername(username: string, page: number = 0, size: number = 12): Observable<VideoPageResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<VideoPageResponse>(`${this.API_URL}/user/${encodeURIComponent(username)}`, { params });
  }

  getPopularVideos(): Observable<PopularVideosResponse> {
    return this.http.get<PopularVideosResponse>('http://localhost:8080/api/etl/popular-videos');
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

  createVideo(formData: FormData, token: string) {
    return this.http.post<any>(
      'http://localhost:8080/api/videos',
      formData,
      {
        headers: {
          Authorization: `Bearer ${token}`
        }
      }
    );
  }

  // Premiere methods
  getPremiereStatus(videoId: number): Observable<PremiereStatus> {
    return this.http.get<PremiereStatus>(`${this.API_URL}/${videoId}/premiere-status`);
  }

  canAccessVideo(videoId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.API_URL}/${videoId}/can-access`);
  }

  joinPremiere(videoId: number): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/${videoId}/premiere-join`, {});
  }

  leavePremiere(videoId: number): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/${videoId}/premiere-leave`, {});
  }


  forceEndPremiere(videoId: number): Observable<PremiereStatus> {
    return this.http.post<PremiereStatus>(`${this.API_URL}/${videoId}/premiere-end`, {});
  }

  formatCountdown(seconds: number): string {
    if (seconds <= 0) return '00:00:00';
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = Math.floor(seconds % 60);
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }
}

