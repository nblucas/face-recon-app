import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { UserResponse, UserService } from '../../services/user-service';
import { EditUserModal } from './edit-user-modal/edit-user-modal';

const PAGE_LIMIT = 20;

@Component({
  selector: 'app-list',
  imports: [EditUserModal],
  templateUrl: './list.html',
  styleUrl: './list.css',
})
export class List {
  private readonly userService = inject(UserService);

  protected readonly users = signal<UserResponse[]>([]);
  protected readonly total = signal(0);
  protected readonly offset = signal(0);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly limit = PAGE_LIMIT;
  protected readonly brokenPictureIds = signal<ReadonlySet<number>>(new Set());
  protected readonly editingUser = signal<UserResponse | null>(null);
  protected readonly pictureVersions = signal<ReadonlyMap<number, number>>(new Map());

  constructor() {
    this.fetchUsers();
  }

  protected get rangeStart(): number {
    return this.total() === 0 ? 0 : this.offset() + 1;
  }

  protected get rangeEnd(): number {
    return Math.min(this.offset() + this.limit, this.total());
  }

  protected get hasPrevious(): boolean {
    return this.offset() > 0;
  }

  protected get hasNext(): boolean {
    return this.offset() + this.limit < this.total();
  }

  protected onPrevious(): void {
    if (!this.hasPrevious) return;
    this.offset.set(Math.max(0, this.offset() - this.limit));
    this.fetchUsers();
  }

  protected onNext(): void {
    if (!this.hasNext) return;
    this.offset.set(this.offset() + this.limit);
    this.fetchUsers();
  }

  protected pictureUrl(user: UserResponse): string {
    const version = this.pictureVersions().get(user.id) ?? 0;
    return `/api/v1/users/${user.id}/picture?v=${version}`;
  }

  protected onPictureError(user: UserResponse): void {
    this.brokenPictureIds.update((ids) => new Set(ids).add(user.id));
  }

  protected formatCpf(cpf: string): string {
    return cpf.replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, '$1.$2.$3-$4');
  }

  protected initialsOf(name: string): string {
    return name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part[0]!.toUpperCase())
      .join('');
  }

  protected onEdit(user: UserResponse): void {
    this.editingUser.set(user);
  }

  protected onEditCancelled(): void {
    this.editingUser.set(null);
  }

  protected onEditSaved(): void {
    const editedUserId = this.editingUser()!.id;

    this.pictureVersions.update((versions) => {
      const next = new Map(versions);
      next.set(editedUserId, (next.get(editedUserId) ?? 0) + 1);
      return next;
    });

    this.editingUser.set(null);
    this.fetchUsers();
  }

  protected onDelete(user: UserResponse): void {
    this.userService.deleteUser(user.id).subscribe({
      next: () => {
        if (this.users().length === 1 && this.offset() > 0) {
          this.offset.set(Math.max(0, this.offset() - this.limit));
        }
        this.fetchUsers();
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(
          typeof err.error === 'string' ? err.error : 'Failed to delete user. Please try again.',
        );
      },
    });
  }

  private fetchUsers(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.brokenPictureIds.set(new Set());

    this.userService.listUsers(this.offset(), this.limit).subscribe({
      next: (page) => {
        this.users.set(page.users);
        this.total.set(page.total);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(
          typeof err.error === 'string' ? err.error : 'Failed to load users. Please try again.',
        );
        this.loading.set(false);
      },
    });
  }
}
