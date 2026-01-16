import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../env/environment';
import { VideoMarker } from '../models/map.model';

export interface MapBounds {
  north: number;
  south: number;
  east: number;
  west: number;
}


export interface TileRequest {
  zoom: number;
  bounds: MapBounds;
}

@Injectable({
  providedIn: 'root'
})
export class MapService {
  private apiUrl = `${environment.apiUrl}/api/map`;

  constructor(private http: HttpClient) {}

  /**
   * Dobavlja video snimke za određeni viewport mape
   * @param tileRequest Parametri za učitavanje tile-a
   * @returns Observable sa video markerima
   */
  getVideosForViewport(tileRequest: TileRequest): Observable<VideoMarker[]> {
    const params = new HttpParams()
      .set('zoom', tileRequest.zoom.toString())
      .set('north', tileRequest.bounds.north.toString())
      .set('south', tileRequest.bounds.south.toString())
      .set('east', tileRequest.bounds.east.toString())
      .set('west', tileRequest.bounds.west.toString());

    return this.http.get<VideoMarker[]>(`${this.apiUrl}/videos`, { params });
  }

  /**
   * Konvertuje lat/lng koordinate u tile koordinate za dati zoom nivo
   * @param lat Geografska širina
   * @param lng Geografska dužina
   * @param zoom Zoom nivo
   * @returns Tile koordinate {x, y}
   */
  latLngToTile(lat: number, lng: number, zoom: number): { x: number; y: number } {
    const x = Math.floor((lng + 180) / 360 * Math.pow(2, zoom));
    const y = Math.floor((1 - Math.log(Math.tan(lat * Math.PI / 180) + 1 / Math.cos(lat * Math.PI / 180)) / Math.PI) / 2 * Math.pow(2, zoom));
    return { x, y };
  }

  /**
   * Konvertuje tile koordinate u lat/lng koordinate
   * @param x Tile X koordinata
   * @param y Tile Y koordinata
   * @param zoom Zoom nivo
   * @returns Geografske koordinate {lat, lng}
   */
  tileToLatLng(x: number, y: number, zoom: number): { lat: number; lng: number } {
    const n = Math.pow(2, zoom);
    const lng = x / n * 360 - 180;
    const latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * y / n)));
    const lat = latRad * 180 / Math.PI;
    return { lat, lng };
  }
}

