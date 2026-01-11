import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Comment } from '../models/comment.model';
import { map } from 'rxjs/operators';
import { environment } from '../env/environment';
@Injectable({
  providedIn: 'root',
})
export class CommentsService {

  private readonly API = `${environment.apiUrl}/api`;
  constructor(private http: HttpClient) {}

  getComments(videoId: number, page = 0, size = 10): Observable<{comments: Comment[]; total: number}> {
    return this.http.get<any>(`${this.API}/videos/${videoId}/comments?page=${page}&size=${size}`).pipe(
      map(resp => ({ comments: resp.content as Comment[], total: resp.totalElements as number }))
    );
  }

  postComment(videoId: number, text: string) {
    return this.http.post<Comment>(`${this.API}/videos/${videoId}/comments`, { text });
  }
}
