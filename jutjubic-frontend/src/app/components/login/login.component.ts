import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { HttpErrorResponse } from '@angular/common/http';

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
    private route: ActivatedRoute
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]],
    });

    // Get return URL from route parameters or default to '/'
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
  }

  onSubmit(): void {
    this.errorMessage = '';

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.authService.login(this.loginForm.value).subscribe({
      next: (response) => {
        // Navigate to return URL or home page
        this.router.navigate([this.returnUrl]);
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting = false;
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
        } else if (error.error?.message) {
          this.errorMessage = error.error.message;
        } else if (error.status === 0) {
          this.errorMessage = 'Ne mogu se povezati sa serverom. Proverite internet konekciju.';
        } else {
          this.errorMessage = 'Pogrešan email ili lozinka.';
        }
      },
      complete: () => {
        this.isSubmitting = false;
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

