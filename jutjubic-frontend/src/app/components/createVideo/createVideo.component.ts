import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-create-video',
  templateUrl: './createVideo.component.html',
  styleUrls: ['./createVideo.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class CreateVideoComponent {
  title = '';
  description = '';
  tags = '';
  videoFile: File | null = null;
  thumbnailFile: File | null = null;
  location = '';

  uploading = false;
  errorMessage = '';

  constructor(
    private videoService: VideoService,
    private authService: AuthService,
    private router: Router
  ) {}

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
      location: this.location
    };

    const formData = new FormData();
    formData.append('data', JSON.stringify(meta));
    formData.append('video', this.videoFile);

    formData.append('thumbnail', this.thumbnailFile!);

    const token = this.authService.getToken();

    console.log('Token:', token);
    console.log('FormData entries:');
    formData.forEach((value, key) => {
      console.log(key, value);
    });

    if (!token) {
      this.errorMessage = 'Nema tokena, prijavite se ponovo.';
      return;
    }

    this.uploading = true;

    this.videoService.createVideo(formData, token).subscribe({
      next: (video) => {
        this.uploading = false;
        console.log('Upload uspeo:', video);
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
