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
    this.videoFile = event.target.files[0];
    this.videoName = this.videoFile ? this.videoFile.name : '';
  }

  onThumbnailSelected(event: any) {
    this.thumbnailFile = event.target.files[0];
    this.thumbnailName = this.thumbnailFile ? this.thumbnailFile.name : '';
  }


  submit() {
    if (!this.title || !this.description || !this.videoFile) {
      this.errorMessage = 'Molimo popunite obavezna polja.';
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

    if (this.thumbnailFile) {
      formData.append('thumbnail', this.thumbnailFile);
    }

    const token = this.authService.getToken();

    // === DEBUG ===
    console.log('Token:', token);
    console.log('FormData entries:');
    formData.forEach((value, key) => {
      console.log(key, value);
    });

    if (!token) {
      this.errorMessage = 'Nema tokena, prijavite se ponovo.';
      return;
    }
    // === END DEBUG ===

    this.uploading = true;

    this.videoService.createVideo(formData, token).subscribe({
      next: (video) => {
        this.uploading = false;
        console.log('Upload uspeo:', video);
        this.router.navigate(['/watch', video.id]); // ide na stranu za gledanje videa
      },
      error: (err) => {
        this.uploading = false;
        this.errorMessage = 'Došlo je do greške prilikom upload-a.';
        console.error('Upload error:', err);
      }
    });
  }
}
