import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { UserResponse, UserService } from '../../services/user-service';

const PAGE_LIMIT = 20;

@Component({
  selector: 'app-list',
  imports: [],
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
    return `/api/v1/users/${user.id}/picture`;
  }

  protected onPictureError(user: UserResponse): void {
    this.brokenPictureIds.update((ids) => new Set(ids).add(user.id));
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
    console.log('Edit user', user);
  }

  protected onDelete(user: UserResponse): void {
    this.users.update((users) => users.filter((u) => u.id !== user.id));
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
