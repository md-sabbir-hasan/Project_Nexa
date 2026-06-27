import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';


export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  userId: number;
  name: string;
  email: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  // Keys for localStorage
  private readonly ACCESS_TOKEN_KEY = 'nexaerp_access_token';
  private readonly REFRESH_TOKEN_KEY = 'nexaerp_refresh_token';
  private readonly USER_KEY = 'nexaerp_user';

  // Login — call API and store tokens
  login(request: LoginRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/auth/login`, request).pipe(
      tap(response => {
        if (response.success) {
          this.storeTokens(response.data);
        }
      })
    );
  }

  // Logout — clear all stored data
  logout(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
  }

  // Store tokens and user info
  private storeTokens(data: LoginResponse): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, data.accessToken);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, data.refreshToken);
    localStorage.setItem(this.USER_KEY, JSON.stringify({
      userId: data.userId,
      name: data.name,
      email: data.email
    }));
  }

  // Get access token
  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  // Get refresh token
  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  // Get current user info
  getCurrentUser(): any {
    const user = localStorage.getItem(this.USER_KEY);
    return user ? JSON.parse(user) : null;
  }

  // Check if user is logged in
  isLoggedIn(): boolean {
    return !!this.getAccessToken();
  }
}