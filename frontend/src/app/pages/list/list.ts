import { Component, signal } from '@angular/core';

interface RegisteredUser {
  name: string;
  cpf: string;
  pictureUrl: string;
}

const MOCK_USERS: RegisteredUser[] = [
  { name: 'Michael Scott', cpf: '123.456.789-00', pictureUrl: 'https://placehold.co/96x96?text=MS' },
  { name: 'Jim Halpert', cpf: '234.567.890-11', pictureUrl: 'https://placehold.co/96x96?text=JH' },
  { name: 'Pam Beesly', cpf: '345.678.901-22', pictureUrl: 'https://placehold.co/96x96?text=PB' },
  { name: 'Dwight Schrute', cpf: '456.789.012-33', pictureUrl: 'https://placehold.co/96x96?text=DS' },
];

@Component({
  selector: 'app-list',
  imports: [],
  templateUrl: './list.html',
  styleUrl: './list.css',
})
export class List {
  protected readonly users = signal<RegisteredUser[]>(MOCK_USERS);

  protected onEdit(user: RegisteredUser): void {
    console.log('Edit user', user);
  }

  protected onDelete(user: RegisteredUser): void {
    this.users.update((users) => users.filter((u) => u.cpf !== user.cpf));
  }
}
