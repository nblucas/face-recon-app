import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { UserService } from '@app/services/user-service';

@Component({
  selector: 'app-verification',
  imports: [ReactiveFormsModule],
  templateUrl: './verification.html',
  styleUrl: './verification.css',
})
export class Verification {
  private readonly userService = inject(UserService);

  protected readonly form = new FormGroup({
    cpf: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    picture: new FormControl<File | null>(null, { validators: [Validators.required] }),
  });

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly matched = signal<boolean | null>(null);

  protected onPictureSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.form.controls.picture.setValue(file);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.matched.set(null);

    const { cpf, picture } = this.form.getRawValue();

    this.userService.verify(cpf, picture!).subscribe({
      next: (response) => {
        this.matched.set(response.matched);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(
          typeof err.error === 'string' ? err.error : 'Failed to verify user. Please try again.',
        );
        this.loading.set(false);
      },
    });
  }
}
