import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface NavItem {
  label: string;
  path: string;
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Register', path: '/register' },
  { label: 'List', path: '/list' },
  { label: 'Verification', path: '/verification' },
  { label: 'Identification', path: '/identification' },
];

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar {
  protected readonly items = NAV_ITEMS;
  protected readonly title = 'FaceRecon';
}
