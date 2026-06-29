import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Account, AccountType, AccountFilter } from '../../models/account';
import { AccountService } from '../../services/account.service';

@Component({
  selector: 'app-account-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './account-list.component.html',
  styleUrl: './account-list.component.scss'
})
export class AccountListComponent implements OnInit {

  private accountService = inject(AccountService);
  protected readonly Math = Math;

  accounts: Account[] = [];
  filteredAccounts: Account[] = [];
  treeAccounts: Account[] = [];
  isLoading = false;
  isTreeView = false;
  showModal = false;
  editingAccount: Account | null = null;

  // Pagination
  currentPage = 1;
  pageSize = 20;
  totalItems = 0;

  // Filter
  filter: AccountFilter = {
    search: '',
    type: '',
    isActive: ''
  };

  // Summary
  summary = {
    total: 0,
    active: 0,
    inactive: 0,
    root: 0
  };

  // Form
  formData = {
    code: '',
    name: '',
    type: '' as AccountType | '',
    description: '',
    parentId: null as number | null
  };

  accountTypes: AccountType[] = ['ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE'];

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.isLoading = true;
    this.accountService.getAll().subscribe({
      next: (data) => {
        this.accounts = data;
        this.applyFilter();
        this.calculateSummary();
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  loadTree(): void {
    this.accountService.getTree().subscribe({
      next: (data) => {
        this.treeAccounts = data;
      }
    });
  }

  applyFilter(): void {
    let result = [...this.accounts];

    if (this.filter.search) {
      const search = this.filter.search.toLowerCase();
      result = result.filter(a =>
        a.name.toLowerCase().includes(search) ||
        a.code.toLowerCase().includes(search)
      );
    }

    if (this.filter.type) {
      result = result.filter(a => a.type === this.filter.type);
    }

    if (this.filter.isActive !== '') {
      result = result.filter(a => a.isActive === this.filter.isActive);
    }

    this.totalItems = result.length;
    this.filteredAccounts = result.slice(
      (this.currentPage - 1) * this.pageSize,
      this.currentPage * this.pageSize
    );
  }

  calculateSummary(): void {
    this.summary = {
      total: this.accounts.length,
      active: this.accounts.filter(a => a.isActive).length,
      inactive: this.accounts.filter(a => !a.isActive).length,
      root: this.accounts.filter(a => !a.parentId).length
    };
  }

  resetFilter(): void {
    this.filter = { search: '', type: '', isActive: '' };
    this.currentPage = 1;
    this.applyFilter();
  }

  toggleView(): void {
    this.isTreeView = !this.isTreeView;
    if (this.isTreeView) {
      this.loadTree();
    }
  }

  openCreateModal(): void {
    this.editingAccount = null;
    this.formData = { code: '', name: '', type: '', description: '', parentId: null };
    this.showModal = true;
  }

  openEditModal(account: Account): void {
    this.editingAccount = account;
    this.formData = {
      code: account.code,
      name: account.name,
      type: account.type,
      description: account.description || '',
      parentId: account.parentId || null
    };
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveAccount(): void {
    if (!this.formData.code || !this.formData.name || !this.formData.type) return;

    const request = {
      code: this.formData.code,
      name: this.formData.name,
      type: this.formData.type as AccountType,
      description: this.formData.description,
      parentId: this.formData.parentId || undefined
    };

    if (this.editingAccount) {
      this.accountService.update(this.editingAccount.id, request).subscribe({
        next: () => {
          this.closeModal();
          this.loadAccounts();
        }
      });
    } else {
      this.accountService.create(request).subscribe({
        next: () => {
          this.closeModal();
          this.loadAccounts();
        }
      });
    }
  }

  deactivate(account: Account): void {
    if (confirm(`Deactivate "${account.name}"?`)) {
      this.accountService.deactivate(account.id).subscribe({
        next: () => this.loadAccounts()
      });
    }
  }

  get totalPages(): number {
    return Math.ceil(this.totalItems / this.pageSize);
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  changePage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.applyFilter();
  }

  getTypeBadgeClass(type: AccountType): string {
    const classes: Record<AccountType, string> = {
      ASSET: 'bg-success',
      LIABILITY: 'bg-danger',
      EQUITY: 'bg-primary',
      REVENUE: 'bg-info',
      EXPENSE: 'bg-warning'
    };
    return classes[type] || 'bg-secondary';
  }

  // Recursive tree render helper
  getTreePadding(level: number): string {
    return `${level * 20}px`;
  }
}