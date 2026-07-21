import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { IdentifyUserResponse, UserResponse, UserService } from '../../services/user-service';
import { formatCpf, initialsOf } from '../../utils/user-display';

@Component({
  selector: 'app-identification',
  imports: [],
  templateUrl: './identification.html',
  styleUrl: './identification.css',
})
export class Identification {
  private readonly userService = inject(UserService);

  protected readonly picture = signal<File | null>(null);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly result = signal<IdentifyUserResponse | null>(null);
  protected readonly pictureBroken = signal(false);

  protected readonly formatCpf = formatCpf;
  protected readonly initialsOf = initialsOf;

  protected onPictureSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.picture.set(file);
  }

  protected onSubmit(): void {
    const picture = this.picture();
    if (!picture) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.result.set(null);
    this.pictureBroken.set(false);

    this.userService.identify(picture).subscribe({
      next: (response) => {
        this.result.set(response);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(
          typeof err.error === 'string' ? err.error : 'Failed to identify user. Please try again.',
        );
        this.loading.set(false);
      },
    });
  }

  protected pictureUrl(user: UserResponse): string {
    return `/api/v1/users/${user.id}/picture?v=${encodeURIComponent(user.updatedAt)}`;
  }

  protected onPictureError(): void {
    this.pictureBroken.set(true);
  }
}
