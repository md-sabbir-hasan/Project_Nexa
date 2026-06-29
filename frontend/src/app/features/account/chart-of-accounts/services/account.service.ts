// features/account/chart-of-accounts/services/account.service.ts

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Account, AccountRequest } from '../models/account';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AccountService {

  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/accounts`;

  // Get all accounts
  getAll(): Observable<Account[]> {
    return this.http.get<any>(this.apiUrl).pipe(
      map(res => res.data)
    );
  }

  // Get tree structure
  getTree(): Observable<Account[]> {
    return this.http.get<any>(`${this.apiUrl}/tree`).pipe(
      map(res => res.data)
    );
  }

  // Get by ID
  getById(id: number): Observable<Account> {
    return this.http.get<any>(`${this.apiUrl}/${id}`).pipe(
      map(res => res.data)
    );
  }

  // Get by type
  getByType(type: string): Observable<Account[]> {
    return this.http.get<any>(`${this.apiUrl}/type/${type}`).pipe(
      map(res => res.data)
    );
  }

  // Create account
  create(request: AccountRequest): Observable<Account> {
    return this.http.post<any>(this.apiUrl, request).pipe(
      map(res => res.data)
    );
  }

  // Update account
  update(id: number, request: AccountRequest): Observable<Account> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, request).pipe(
      map(res => res.data)
    );
  }

  // Deactivate account
  deactivate(id: number): Observable<void> {
    return this.http.patch<any>(`${this.apiUrl}/${id}/deactivate`, {});
  }
}