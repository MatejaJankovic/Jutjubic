// typescript
// File: jutjubic-frontend/src/app/components/profile/profile.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { UserService } from '../../services/user.service';
import { UserProfile } from '../../models/user-profile.model';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  username = '';
  profile: UserProfile | null = null;
  loading = true;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.username = this.route.snapshot.paramMap.get('username') || '';
    if (!this.username) {
      this.error = 'Nevažeći korisnik';
      this.loading = false;
      return;
    }

    this.userService.getProfile(this.username).subscribe({
      next: (p) => {
        this.profile = p;
        this.loading = false;
      },
      error: () => {
        this.error = 'Korisnik nije pronađen';
        this.loading = false;
      }
    });
  }
}
