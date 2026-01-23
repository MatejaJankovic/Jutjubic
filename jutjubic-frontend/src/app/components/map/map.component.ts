import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import * as L from 'leaflet';
import { MapService } from '../../services/map.service';
import { TILE_CONFIGS, ZoomLevel, VideoMarker } from '../../models/map.model';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.scss']
})
export class MapComponent implements OnInit, OnDestroy {
  private map!: L.Map;
  private markers: L.Marker[] = [];
  private readonly DEFAULT_LAT = 45.2671; // Novi Sad
  private readonly DEFAULT_LNG = 19.8335; // Novi Sad
  private readonly DEFAULT_ZOOM = 8; // Veći zoom da se vide lokalni videi

  startDate?: string; // format yyyy-MM-dd
  endDate?: string;

  constructor(
    private mapService: MapService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initMap();
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }
   // Auto refres kada se datum promeni
    onDateChange(): void {
      this.loadVideosForCurrentView();
    }

    clearDates() {
      this.startDate = '';
      this.endDate = '';

      this.loadVideosForCurrentView();
    }

  private initMap(): void {
    // Kreiranje mape sa default view na Evropu
    this.map = L.map('map', {
      center: [this.DEFAULT_LAT, this.DEFAULT_LNG],
      zoom: this.DEFAULT_ZOOM,
      minZoom: 3,
      maxZoom: 15
    });

    // Dodavanje OpenStreetMap tile layer-a
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 18
    }).addTo(this.map);

    // Event listener za pracenje trenutnog viewport-a
    this.map.on('moveend', () => this.onMapMoved());
    this.map.on('zoomend', () => this.onMapZoomed());

    // Fix za Leaflet default marker icon
    this.fixLeafletDefaultIcon();

    // Učitaj inicijalne videe
    this.loadVideosForCurrentView();
  }

  private fixLeafletDefaultIcon(): void {
    // Fix za Leaflet default marker icon path
    const iconDefault = L.icon({
      iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
      iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
      shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      shadowSize: [41, 41]
    });
    L.Marker.prototype.options.icon = iconDefault;
  }

  private onMapMoved(): void {
    this.loadVideosForCurrentView();
  }

  private onMapZoomed(): void {
    this.loadVideosForCurrentView();
  }

  private loadVideosForCurrentView(): void {
    const bounds = this.map.getBounds();
    const zoom = this.map.getZoom();

    const tileRequest = {
      zoom,
      bounds: {
        north: bounds.getNorth(),
        south: bounds.getSouth(),
        east: bounds.getEast(),
        west: bounds.getWest()
      },
      startDate: this.startDate ? this.startDate : undefined,
      endDate: this.endDate ? this.endDate : undefined
    };

    this.mapService.getVideosForViewport(tileRequest).subscribe({
      next: (videos) => {
        this.clearMarkers();
        this.addVideoMarkers(videos);
      },
      error: (err) => {
        console.error('Error loading videos for map:', err);
      }
    });
  }

  private clearMarkers(): void {
    this.markers.forEach(marker => {
      this.map.removeLayer(marker);
    });
    this.markers = [];
  }

  /**
   * Deduplicates videos that would render in the same geographic cell.
   * Uses geographic grid (lat/lng) for stable marker placement.
   *
   * @param videos List of videos to deduplicate
   * @returns Filtered list with no overlapping markers
   */
  private dedupeStackedVideos(videos: VideoMarker[]): VideoMarker[] {
    const zoom = this.map.getZoom();

    // Geographic cell size in degrees (smaller = more videos shown)
    // Zoom 1-5: 0.15° cells (~16.5km) - allows ~20 videos
    // Zoom 6-10: 0.05° cells (~5.5km) - allows ~30 videos
    // Zoom 11+: 0.01° cells (~1.1km) - allows ~50+ videos
    const cellSizeDegrees = zoom >= 11 ? 0.01 : zoom >= 6 ? 0.05 : 0.15;

    // Sort by viewCount descending so higher priority videos are processed first
    const sortedVideos = [...videos].sort((a, b) => (b.viewCount || 0) - (a.viewCount || 0));

    const cellMap = new Map<string, VideoMarker>();
    const selectedVideos: VideoMarker[] = [];

    sortedVideos.forEach(video => {
      if (!video.latitude || !video.longitude) {
        return;
      }

      // Calculate which geographic cell this video belongs to
      const cellLat = Math.floor(video.latitude / cellSizeDegrees);
      const cellLng = Math.floor(video.longitude / cellSizeDegrees);
      const cellKey = `${cellLat}:${cellLng}`;

      // Only add if this cell is empty (first come, first served based on viewCount)
      if (!cellMap.has(cellKey)) {
        cellMap.set(cellKey, video);
        selectedVideos.push(video);
      }
    });

    console.log(`[Deduplication] Zoom: ${zoom}, CellSize: ${cellSizeDegrees}°, Input: ${videos.length}, Output: ${selectedVideos.length}`);

    return selectedVideos;
  }

  private addVideoMarkers(videos: VideoMarker[]): void {
    // Deduplicate stacked markers to prevent overlapping
    const dedupedVideos = this.dedupeStackedVideos(videos);

    dedupedVideos.forEach(video => {
      if (video.latitude && video.longitude) {
        const marker = this.createVideoMarker(video);
        marker.addTo(this.map);
        this.markers.push(marker);
      }
    });
  }

  private createVideoMarker(video: VideoMarker): L.Marker {
    const videoIcon = L.divIcon({
      html: '<div style="font-size: 32px;">🎬</div>',
      className: 'video-marker-icon',
      iconSize: [40, 40],
      iconAnchor: [20, 40],
      popupAnchor: [0, -40]
    });

    const marker = L.marker([video.latitude, video.longitude], {
      icon: videoIcon
    });

    const popupContent = this.createPopupContent(video);
    const popup = L.popup({
      maxWidth: 300,
      className: 'modern-video-popup'
    }).setContent(popupContent);

    marker.bindPopup(popup);

    marker.on('popupopen', () => {
      setTimeout(() => {
        const watchBtn = document.querySelector('.popup-watch-btn') as HTMLButtonElement;
        if (watchBtn) {
          watchBtn.addEventListener('click', (event) => {
            event.preventDefault();
            event.stopPropagation();
            this.router.navigate(['/watch', video.id]);
          });
        }
      }, 100);
    });

    return marker;
  }

  private createPopupContent(video: VideoMarker): string {
    const thumbnailUrl = video.thumbnailUrl.startsWith('http')
      ? video.thumbnailUrl
      : `http://localhost:8080${video.thumbnailUrl}`;

    const viewCount = video.viewCount || 0;
    const uploadDate = new Date(video.createdAt).toLocaleDateString('sr-RS');

    return `
      <div class="video-popup-card">
        <div class="popup-thumbnail">
          <img src="${thumbnailUrl}" alt="${video.title}" />
        </div>
        <div class="popup-content">
          <h3 class="popup-title">${this.escapeHtml(video.title)}</h3>
          <div class="popup-meta">
            <span class="popup-username">👤 ${this.escapeHtml(video.username)}</span>
            <span class="popup-views">👁️ ${viewCount.toLocaleString()}</span>
          </div>
          <div class="popup-date">📅 ${uploadDate}</div>
          <button class="popup-watch-btn" data-video-id="${video.id}">
            ▶️ Watch Video
          </button>
        </div>
      </div>
    `;
  }

  private escapeHtml(text: string): string {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  private getZoomLevel(zoom: number): ZoomLevel {
    if (zoom >= TILE_CONFIGS.detailed.minZoom) {
      return 'detailed';
    } else if (zoom >= TILE_CONFIGS.medium.minZoom) {
      return 'medium';
    } else {
      return 'overview';
    }
  }
}

