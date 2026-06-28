import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {

  private cdr = inject(ChangeDetectorRef);

  stats = {
    totalReceivable: 0,
    totalPayable: 0,
    cashBalance: 0,
    bankBalance: 0
  };

  isLoading = true;

  ngOnInit(): void {
    setTimeout(() => {
      this.stats = {
        totalReceivable: 89000,
        totalPayable: 500,
        cashBalance: 0,
        bankBalance: 50000
      };
      this.isLoading = false;
      this.cdr.detectChanges(); // ← এটা যোগ করো
    }, 500);
  }
}