import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface UserResponse {
  name: string;
  cpf: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/users';

  createUser(name: string, cpf: string, picture: File): Observable<UserResponse> {
    const request = new Blob([JSON.stringify({ name, cpf })], { type: 'application/json' });

    const formData = new FormData();
    formData.append('request', request);
    formData.append('picture', picture);

    return this.http.post<UserResponse>(this.baseUrl, formData);
  }
}
