import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-footer-template',
  templateUrl: './footer-template.component.html',
  styleUrls: ['./footer-template.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class FooterTemplateComponent implements OnInit {

  constructor() { }

  ngOnInit(): void {
  }

}
