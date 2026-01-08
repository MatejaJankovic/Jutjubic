import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet, RouterLinkActive, NavigationEnd } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { AuthResponse } from '../../models/auth.model';
import { Subscription, filter } from 'rxjs';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, RouterLinkActive, FormsModule],
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss'],
})
export class LayoutComponent implements OnInit, OnDestroy {
  currentUser: AuthResponse | null = null;
  sidebarExpanded = false;
  searchQuery = '';
  private subscription = new Subscription();

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.subscription.add(
      this.authService.currentUser$.subscribe((user) => {
        this.currentUser = user;
      })
    );

    // Close sidebar on route change
    this.subscription.add(
      this.router.events.pipe(
        filter(event => event instanceof NavigationEnd)
      ).subscribe(() => {
        this.sidebarExpanded = false;
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  toggleSidebar(): void {
    this.sidebarExpanded = !this.sidebarExpanded;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }

  onSearch(): void {
    if (this.searchQuery.trim()) {
      this.router.navigate(['/'], { queryParams: { search: this.searchQuery } });
    } else {
      this.router.navigate(['/']);
    }
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.router.navigate(['/']);
  }
}
