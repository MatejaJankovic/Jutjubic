import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as L from 'leaflet';
import { MapService } from '../../services/map.service';
import { TILE_CONFIGS, ZoomLevel } from '../../models/map.model';

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.scss']
})
export class MapComponent implements OnInit, OnDestroy {
  private map!: L.Map;
  private readonly DEFAULT_LAT = 48.2082; // Centar Evrope (približno)
  private readonly DEFAULT_LNG = 16.3738;
  private readonly DEFAULT_ZOOM = 5;

  constructor(private mapService: MapService) {}

  ngOnInit(): void {
    this.initMap();
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
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
  }

  private fixLeafletDefaultIcon(): void {
    // Fix za Leaflet default marker icon path
    const iconRetinaUrl = 'assets/marker-icon-2x.png';
    const iconUrl = 'assets/marker-icon.png';
    const shadowUrl = 'assets/marker-shadow.png';
    const iconDefault = L.icon({
      iconRetinaUrl,
      iconUrl,
      shadowUrl,
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      tooltipAnchor: [16, -28],
      shadowSize: [41, 41]
    });
    L.Marker.prototype.options.icon = iconDefault;
  }

  private onMapMoved(): void {
    const bounds = this.map.getBounds();
    const zoom = this.map.getZoom();

    console.log('Map moved - Bounds:', {
      north: bounds.getNorth(),
      south: bounds.getSouth(),
      east: bounds.getEast(),
      west: bounds.getWest()
    }, 'Zoom:', zoom);

    // Ovde će kasnije biti poziv ka backend-u za učitavanje videa
    // this.loadVideosForCurrentView();
  }

  private onMapZoomed(): void {
    const zoom = this.map.getZoom();
    const zoomLevel = this.getZoomLevel(zoom);
    console.log('Map zoom changed:', zoom, 'Level:', zoomLevel);
    // Ovde će kasnije biti logika za prebacivanje između nivoa tile-ova
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

