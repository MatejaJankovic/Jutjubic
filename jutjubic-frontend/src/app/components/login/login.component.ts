import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent {
  loginForm: FormGroup;
  errorMessage: string = '';
  isSubmitting: boolean = false;
  returnUrl: string = '/';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]],
    });

    // Get return URL from route parameters or default to '/'
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
  }

  onSubmit(): void {
    // Clear previous error
    this.errorMessage = '';

    // Validate form
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.cdr.detectChanges(); // Force UI update to show loading state

    this.authService.login(this.loginForm.value)
      .pipe(
        finalize(() => {
          // This will ALWAYS run, whether success or error
          this.isSubmitting = false;
          this.cdr.detectChanges(); // Force UI update to hide loading state
        })
      )
      .subscribe({
        next: () => {
          // Navigate to return URL or home page
          this.router.navigate([this.returnUrl]);
        },
        error: (error: HttpErrorResponse) => {
          // Handle different error types
          if (error.status === 429) {
            // Rate limit exceeded
            const retryAfter = error.error?.retryAfter;
            if (retryAfter) {
              this.errorMessage = `Previše pokušaja prijave. Pokušajte ponovo za ${retryAfter} sekundi.`;
            } else if (error.error?.message) {
              this.errorMessage = error.error.message;
            } else {
              this.errorMessage = 'Previše pokušaja prijave. Molimo sačekajte.';
            }
          } else if (error.status === 400) {
            // Bad request - invalid credentials or disabled account
            this.errorMessage = error.error?.message || 'Pogrešan email ili lozinka.';
          } else if (error.status === 0) {
            // Network error
            this.errorMessage = 'Ne mogu se povezati sa serverom. Proverite internet konekciju.';
          } else if (error.error?.message) {
            // Any other error with message
            this.errorMessage = error.error.message;
          } else {
            // Generic error
            this.errorMessage = 'Došlo je do greške. Pokušajte ponovo.';
          }

          // Force immediate UI update to show error message
          this.cdr.detectChanges();
        },
      });
  }

  getFieldError(fieldName: string): string | null {
    const control = this.loginForm.get(fieldName);

    if (control && control.invalid && (control.dirty || control.touched)) {
      if (control.errors?.['required']) {
        return fieldName === 'email' ? 'Email je obavezan' : 'Lozinka je obavezna';
      }
      if (control.errors?.['email']) {
        return 'Email format nije validan';
      }
    }

    return null;
  }

  hasFieldError(fieldName: string): boolean {
    return !!this.getFieldError(fieldName);
  }
}

