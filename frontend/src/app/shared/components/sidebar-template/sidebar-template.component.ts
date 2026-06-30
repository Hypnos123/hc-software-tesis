import { Component, OnInit } from '@angular/core';
import { IItemMenu } from '../sidebar/models/sidebar';
import { AuthService } from '@app/auth/services/auth.service';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-sidebar-template',
  templateUrl: './sidebar-template.component.html',
  styleUrls: ['./sidebar-template.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule]
})
export class SidebarTemplateComponent implements OnInit {

  menu: IItemMenu[] = [];
  usuario!: any;
  mostrarDashboard = false;


  constructor(private authService: AuthService) { }

  ngOnInit(): void {
    //this.getUsuario();
    this.getMenu();
  }

  // getUsuario () {
  //   this.usuario = this.authService.usuario;
  // }

  getMenu() {
    //this.menu = this.authService.permisos;
  }
}
