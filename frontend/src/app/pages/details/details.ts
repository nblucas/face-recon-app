import { Component, effect, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { UserResponse, UserService } from '../../services/user-service';
import { EditUserModal } from '../list/edit-user-modal/edit-user-modal';
import { formatCpf, initialsOf } from '../../utils/user-display';

@Component({
  selector: 'app-details',
  imports: [EditUserModal],
  templateUrl: './details.html',
  styleUrl: './details.css',
})
export class Details {
  private readonly router = inject(Router);
  private readonly userService = inject(UserService);

  readonly id = input.required<string>();

  protected readonly user = signal<UserResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly editing = signal(false);
  protected readonly pictureBroken = signal(false);
  protected readonly pictureVersion = signal(0);

  protected readonly formatCpf = formatCpf;
  protected readonly initialsOf = initialsOf;

  constructor() {
    effect(() => {
      this.fetchUser(Number(this.id()));
    });
  }

  protected pictureUrl(user: UserResponse): string {
    return `/api/v1/users/${user.id}/picture?v=${this.pictureVersion()}`;
  }

  protected onPictureError(): void {
    this.pictureBroken.set(true);
  }

  protected onEdit(): void {
    this.editing.set(true);
  }

  protected onEditCancelled(): void {
    this.editing.set(false);
  }

  protected onEditSaved(): void {
    this.editing.set(false);
    this.pictureVersion.update((v) => v + 1);
    this.fetchUser(Number(this.id()));
  }

  protected onDelete(): void {
    const id = this.user()!.id;

    this.userService.deleteUser(id).subscribe({
      next: () => this.router.navigate(['/list']),
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(
          typeof err.error === 'string' ? err.error : 'Failed to delete user. Please try again.',
        );
      },
    });
  }

  private fetchUser(id: number): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.pictureBroken.set(false);

    this.userService.getUser(id).subscribe({
      next: (user) => {
        this.user.set(user);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(
          typeof err.error === 'string' ? err.error : 'Failed to load user. Please try again.',
        );
        this.loading.set(false);
      },
    });
  }
}
