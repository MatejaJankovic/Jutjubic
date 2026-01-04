import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
})
export class RegisterComponent {
  registerForm: FormGroup;
  errorMessage: string = '';
  validationErrors: { [key: string]: string } = {};
  successMessage: string = '';
  isSubmitting: boolean = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.registerForm = this.fb.group(
      {
        email: ['', [Validators.required, Validators.email]],
        username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
        password: ['', [Validators.required, Validators.minLength(6)]],
        confirmPassword: ['', [Validators.required]],
        firstName: ['', [Validators.required]],
        lastName: ['', [Validators.required]],
        address: ['', [Validators.required]],
      },
      { validators: this.passwordMatchValidator }
    );
  }

  passwordMatchValidator(form: FormGroup) {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');

    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }

    return null;
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.validationErrors = {};
    this.successMessage = '';

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.authService.register(this.registerForm.value).subscribe({
      next: (response) => {
        this.successMessage = response.message;
        this.registerForm.reset();
        // Optionally redirect to login after a few seconds
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 3000);
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting = false;
        if (error.status === 400 && error.error.errors) {
          // Validation errors
          this.validationErrors = error.error.errors;
          this.errorMessage = 'Molimo ispravite greške u formi.';
        } else if (error.error?.message) {
          this.errorMessage = error.error.message;
        } else {
          this.errorMessage = 'Došlo je do greške. Pokušajte ponovo.';
        }
      },
      complete: () => {
        this.isSubmitting = false;
      },
    });
  }

  getFieldError(fieldName: string): string | null {
    const control = this.registerForm.get(fieldName);

    // Check for server-side validation errors first
    if (this.validationErrors[fieldName]) {
      return this.validationErrors[fieldName];
    }

    // Check for client-side validation errors
    if (control && control.invalid && (control.dirty || control.touched)) {
      if (control.errors?.['required']) {
        return this.getRequiredMessage(fieldName);
      }
      if (control.errors?.['email']) {
        return 'Email format nije validan';
      }
      if (control.errors?.['minlength']) {
        const minLength = control.errors['minlength'].requiredLength;
        return `Minimum ${minLength} karaktera`;
      }
      if (control.errors?.['maxlength']) {
        const maxLength = control.errors['maxlength'].requiredLength;
        return `Maksimum ${maxLength} karaktera`;
      }
      if (control.errors?.['passwordMismatch']) {
        return 'Lozinke se ne poklapaju';
      }
    }

    return null;
  }

  private getRequiredMessage(fieldName: string): string {
    const messages: { [key: string]: string } = {
      email: 'Email je obavezan',
      username: 'Korisničko ime je obavezno',
      password: 'Lozinka je obavezna',
      confirmPassword: 'Potvrda lozinke je obavezna',
      firstName: 'Ime je obavezno',
      lastName: 'Prezime je obavezno',
      address: 'Adresa je obavezna',
    };
    return messages[fieldName] || 'Ovo polje je obavezno';
  }

  hasFieldError(fieldName: string): boolean {
    return !!this.getFieldError(fieldName);
  }
}

