// typescript
// File: jutjubic-frontend/src/app/services/user.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserProfile } from '../models/user-profile.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private base = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getProfile(username: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.base}/users/${encodeURIComponent(username)}`);
  }

  // Optional: get user's videos (implement if backend supports it)
  // getUserVideos(username: string, page = 0, size = 12) { ... }
}
