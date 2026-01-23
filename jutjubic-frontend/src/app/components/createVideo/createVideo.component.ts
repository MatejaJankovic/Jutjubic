import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';

@Component({
  selector: 'app-create-video',
  templateUrl: './createVideo.component.html',
  styleUrls: ['./createVideo.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class CreateVideoComponent implements OnInit, OnDestroy {
  title = '';
  description = '';
  tags = '';
  videoFile: File | null = null;
  thumbnailFile: File | null = null;
  location = '';
  latitude: number | null = null;
  longitude: number | null = null;

  uploading = false;
  errorMessage = '';

  private map!: L.Map;
  private marker!: L.Marker;

  constructor(
    private videoService: VideoService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Initialize map after view is ready
    setTimeout(() => this.initMap(), 100);
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  private initMap(): void {
    // Create map with default view
    this.map = L.map('location-map', {
      center: [45.2671, 19.8335], // Novi Sad coordinates as default
      zoom: 13, // Veći zoom za bolju preciznost
      minZoom: 2,
      maxZoom: 18
    });

    // Add OpenStreetMap tile layer
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 18
    }).addTo(this.map);

    // Fix for Leaflet default marker icon
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

    // Add click listener to place marker
    this.map.on('click', (e: L.LeafletMouseEvent) => {
      this.onMapClick(e);
    });
  }

  private onMapClick(e: L.LeafletMouseEvent): void {
    const { lat, lng } = e.latlng;
    this.latitude = lat;
    this.longitude = lng;

    // Remove existing marker if any
    if (this.marker) {
      this.map.removeLayer(this.marker);
    }

    // Add new marker
    this.marker = L.marker([lat, lng], {
      draggable: true
    }).addTo(this.map);

    // Update coordinates when marker is dragged
    this.marker.on('dragend', () => {
      const position = this.marker.getLatLng();
      this.latitude = position.lat;
      this.longitude = position.lng;
    });

    // Add popup to marker
    this.marker.bindPopup(`Lokacija: ${lat.toFixed(4)}, ${lng.toFixed(4)}`).openPopup();
  }

  videoName: string = '';
  thumbnailName: string = '';

  onVideoSelected(event: any) {
    const file: File = event.target.files[0];

    if (!file) {
      this.videoFile = null;
      this.videoName = '';
      return;
    }

    const maxSize = 200 * 1024 * 1024; // 200MB
    if (file.size > maxSize) {
      this.errorMessage = 'Video ne sme biti veći od 200MB';
      this.videoFile = null;
      this.videoName = '';
      return;
    }

    // Provera tipa
    if (file.type !== 'video/mp4') {
      this.errorMessage = 'Dozvoljen je samo mp4 format';
      this.videoFile = null;
      this.videoName = '';
      return;
    }

    this.videoFile = file;
    this.videoName = file.name;
    this.errorMessage = '';
  }


  onThumbnailSelected(event: any) {
    this.thumbnailFile = event.target.files[0];
    this.thumbnailName = this.thumbnailFile ? this.thumbnailFile.name : '';
  }


  submit() {
    if (!this.title || !this.description || !this.videoFile || !this.thumbnailFile) {
        this.errorMessage = 'Naslov, opis, video i thumbnail su obavezni.';
        return;
      }

    const meta = {
      title: this.title,
      description: this.description,
      tags: this.tags.split(',').map(t => t.trim()),
      location: this.location,
      latitude: this.latitude,
      longitude: this.longitude
    };

    const formData = new FormData();
    formData.append('data', JSON.stringify(meta));
    formData.append('video', this.videoFile);
    formData.append('thumbnail', this.thumbnailFile!);

    const token = this.authService.getToken();

    if (!token) {
      this.errorMessage = 'Nema tokena, prijavite se ponovo.';
      return;
    }

    this.uploading = true;

    this.videoService.createVideo(formData, token).subscribe({
      next: (video) => {
        this.uploading = false;
        this.router.navigate(['']);
      },
      error: (err) => {
        this.uploading = false;

        if (err.error && err.error.message) {
          this.errorMessage = err.error.message;
        } else if (err.status === 0) {
          this.errorMessage = 'Upload nije uspeo. Proveri veličinu fajla (max 200MB) ili konekciju.';
        } else {
          this.errorMessage = err.error ? err.error : 'Došlo je do greške prilikom upload-a.';
        }

        console.error('Upload error:', err);
      }

    });
  }
}
