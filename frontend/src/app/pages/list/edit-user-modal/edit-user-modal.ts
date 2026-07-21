import { Component, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { UserResponse, UserService } from '@app/services/user-service';

@Component({
  selector: 'app-edit-user-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './edit-user-modal.html',
  styleUrl: './edit-user-modal.css',
})
export class EditUserModal {
  private readonly userService = inject(UserService);

  readonly user = input.required<UserResponse>();
  readonly saved = output<void>();
  readonly cancelled = output<void>();

  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true }),
    picture: new FormControl<File | null>(null),
  });

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected onPictureSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.form.controls.picture.setValue(file);
  }

  protected onCancel(): void {
    this.cancelled.emit();
  }

  protected onSubmit(): void {
    const { name, picture } = this.form.getRawValue();
    const trimmedName = name.trim();

    if (!trimmedName && !picture) {
      this.errorMessage.set('Fill in at least the name or the picture.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.userService.updateUser(this.user().id, trimmedName || null, picture).subscribe({
      next: () => this.saved.emit(),
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(
          typeof err.error === 'string' ? err.error : 'Failed to update user. Please try again.',
        );
        this.loading.set(false);
      },
    });
  }
}
