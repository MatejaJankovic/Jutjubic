import { Component, OnInit, OnDestroy, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { WatchPartyService } from '../../services/watch-party.service';

@Component({
  selector: 'app-watch-party-join',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './watch-party-join.component.html',
  styleUrls: ['./watch-party-join.component.scss']
})
export class WatchPartyJoinComponent implements OnInit, OnDestroy {
  loading = true;
  error = '';
  inviteCode = '';

  private subscriptions = new Subscription();

  constructor(
    private watchPartyService: WatchPartyService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    // Get invite code from route parameter
    this.inviteCode = this.route.snapshot.paramMap.get('inviteCode') || '';

    if (!this.inviteCode) {
      this.error = 'Invalid invite code';
      this.loading = false;
      this.cdr.detectChanges();
      return;
    }

    // Join the room
    this.joinRoom();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  private joinRoom(): void {
    this.loading = true;
    this.error = '';
    this.cdr.detectChanges();

    this.subscriptions.add(
      this.watchPartyService.joinRoom(this.inviteCode).subscribe({
        next: (room) => {
          console.log('Joined watch party:', room);
          // Navigate to the room page
          this.router.navigate(['/watch-party/room', room.id]);
        },
        error: (err) => {
          this.ngZone.run(() => {
            this.loading = false;
            this.error = 'Failed to join watch party. Invalid invite code or room not found.';
            this.cdr.detectChanges();
          });
          console.error('Error joining watch party:', err);
        }
      })
    );
  }

  goToCreate(): void {
    this.router.navigate(['/watch-party']);
  }

  goHome(): void {
    this.router.navigate(['/']);
  }
}

