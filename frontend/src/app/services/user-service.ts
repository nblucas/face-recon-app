import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface UserResponse {
  id: number;
  name: string;
  cpf: string;
  createdAt: string;
}

export interface UserPageResponse {
  users: UserResponse[];
  total: number;
  offset: number;
  limit: number;
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

  listUsers(offset: number, limit: number): Observable<UserPageResponse> {
    const params = new HttpParams().set('offset', offset).set('limit', limit);
    return this.http.get<UserPageResponse>(this.baseUrl, { params });
  }
}
