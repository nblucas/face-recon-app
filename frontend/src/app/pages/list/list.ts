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
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.fetchUsers();
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

    this.userService.listUsers(0, PAGE_LIMIT).subscribe({
      next: (page) => {
        this.users.set(page.users);
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
