// features/account/chart-of-accounts/models/account.ts

export interface Account {
  id: number;
  code: string;
  name: string;
  description?: string;
  type: AccountType;
  isActive: boolean;
  isDefault: boolean;
  parentId?: number;
  parentName?: string;
  currentBalance: number;
  children?: Account[];
}

export type AccountType =
  'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE';

export interface AccountRequest {
  code: string;
  name: string;
  description?: string;
  type: AccountType;
  parentId?: number;
}

export interface AccountFilter {
  search?: string;
  type?: AccountType | '';
  isActive?: boolean | '';
  parentId?: number | '';
}