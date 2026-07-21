import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';

export interface UserResponse {
  id: number;
  name: string;
  cpf: string;
  createdAt: string;
  updatedAt: string;
}

export interface UserPageResponse {
  users: UserResponse[];
  total: number;
  offset: number;
  limit: number;
}

export interface IdentifyUserResponse {
  identified: boolean;
  user: UserResponse | null;
}

export interface VerifyUserResponse {
  matched: boolean;
}

export interface CreateUsersBatchEntry {
  name: string;
  cpf: string;
  picture: File;
}

export interface CreateUsersBatchResponse {
  users: UserResponse[];
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/users';

  updateUser(id: number, name: string | null, picture: File | null): Observable<UserResponse> {
    if (!name && !picture) {
      return throwError(() => new Error('At least one of name or picture must be given.'));
    }

    const requestBody: { name?: string } = {};
    if (name) {
      requestBody.name = name;
    }
    const request = new Blob([JSON.stringify(requestBody)], { type: 'application/json' });

    const formData = new FormData();
    formData.append('request', request);
    if (picture) {
      formData.append('picture', picture);
    }

    return this.http.put<UserResponse>(`${this.baseUrl}/${id}`, formData);
  }

  listUsers(offset: number, limit: number): Observable<UserPageResponse> {
    const params = new HttpParams().set('offset', offset).set('limit', limit);
    return this.http.get<UserPageResponse>(this.baseUrl, { params });
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getUser(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/${id}`);
  }

  identify(picture: File): Observable<IdentifyUserResponse> {
    const formData = new FormData();
    formData.append('picture', picture);

    return this.http.post<IdentifyUserResponse>(`${this.baseUrl}/identify`, formData);
  }

  verify(cpf: string, picture: File): Observable<VerifyUserResponse> {
    const request = new Blob([JSON.stringify({ cpf })], { type: 'application/json' });

    const formData = new FormData();
    formData.append('request', request);
    formData.append('picture', picture);

    return this.http.post<VerifyUserResponse>(`${this.baseUrl}/verify`, formData);
  }

  createUsersBatch(entries: CreateUsersBatchEntry[]): Observable<CreateUsersBatchResponse> {
    const users = entries.map(({ name, cpf }, index) => ({ clientId: String(index), name, cpf }));
    const request = new Blob([JSON.stringify({ users })], { type: 'application/json' });

    const formData = new FormData();
    formData.append('request', request);
    entries.forEach(({ picture }, index) => {
      const renamedPicture = new File([picture], `${index}${fileExtension(picture.name)}`, { type: picture.type });
      formData.append('pictures', renamedPicture);
    });

    return this.http.post<CreateUsersBatchResponse>(`${this.baseUrl}/batch`, formData);
  }
}

function fileExtension(filename: string): string {
  const dotIndex = filename.lastIndexOf('.');
  return dotIndex >= 0 ? filename.slice(dotIndex) : '';
}
