import { Component, OnInit } from '@angular/core';
import { IItemMenu } from '../sidebar/models/sidebar';
import { AuthService } from '@app/auth/services/auth.service';
import { SHARED_ALL } from '@app/shared/shared.config';

@Component({
  selector: 'app-sidebar-template',
  templateUrl: './sidebar-template.component.html',
  styleUrls: ['./sidebar-template.component.scss'],
  standalone: true,
  imports: [SHARED_ALL]
})
export class SidebarTemplateComponent implements OnInit {

  menu: IItemMenu[] = [];
  usuario!: any;


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
