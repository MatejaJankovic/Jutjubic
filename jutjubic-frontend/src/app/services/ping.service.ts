import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PingService {
  private readonly url = 'http://localhost:8080/api/ping';

  constructor(private http: HttpClient) {}

  ping(): Observable<string> {
    return this.http.get(this.url, { responseType: 'text' }) as Observable<string>;
  }
}
