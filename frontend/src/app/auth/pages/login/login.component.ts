import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { IAuth } from '@app/auth/models/auth';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ButtonModule, InputTextModule]
})
export class LoginComponent implements OnInit {
  iconEye: string = 'pi pi-eye';
  isLoading = false;
  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private readonly servicioMensajesSwal: MensajesSwalService,
  ) { }

  ngOnInit(): void {
    this.loginForm.reset();
  }

  loginForm = this.fb.group({
    usuario: [null, [Validators.required]],
    contrasena: [null, [Validators.required]],
    recordarme: [false],
  });

  get usuario() {
    return this.loginForm.get('usuario');
  }

  get contrasena() {
    return this.loginForm.get('contrasena');
  }

  


  iniciarSesion() {
    const header = this.loginForm.value as IAuth;
    this.isLoading = true;

    this.authService.login(header).subscribe({
      next: (auth) => {
        if (auth?.usuario) {
          setTimeout(() => {
            this.router.navigateByUrl(this.authService.getRutaInicialPermitida());
            this.isLoading = false;
          }, 2000);
          return;
        }

        this.servicioMensajesSwal.mensajeAdvertencia('Verifique usuario y contraseña');
        this.isLoading = false;
        this.loginForm.reset();
      },
      error: (error) => {
        this.servicioMensajesSwal.mensajeError(error);
        this.isLoading = false;
        this.loginForm.reset();
      }
    });
  }

  viewPassword(input: any) {
    input.type = input.type === 'password' ? 'text' : 'password';
    this.iconEye = input.type === 'password' ? 'pi pi-eye' : 'pi pi-eye-slash';
  }

}
