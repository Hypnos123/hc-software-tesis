
export const getLogin = () => {
  return [{
    usuario: {
      apellido: 'Munayco',
      idUsuario: 101,
      nombre: 'Kristel',
      tipoUsuario: 'Administrador'
    },
    detallePermisos: [
      {
        nombre: 'Usuarios',
        ruta: '/usuarios',
        idMenu: 2,
        imagen: 'assets/icons/users.svg'
      },
    ],
    mensaje: 'Autenticación exitosa'
  }]
}

export const getAllUsuarios = () => {
  return [
    {
      idUsuario: 1,
      usuario: 'kristel.munayco',
      contrasena: 'Password123!',
      tipoUsuario: 'admin',
      estado: true,
      idEmpleado: 101,
      apellidoYNombre: 'Munayco Kristel',
      idGenerado: 1001
    },
    {
      idUsuario: 2,
      usuario: 'juan.perez',
      contrasena: 'Juan2025!',
      tipoUsuario: 'usuario',
      estado: true,
      idEmpleado: 102,
      apellidoYNombre: 'Perez Juan',
      idGenerado: 1002
    },
    {
      idUsuario: 3,
      usuario: 'maria.lopez',
      contrasena: 'Maria@123',
      tipoUsuario: 'usuario',
      estado: false,
      idEmpleado: 103,
      apellidoYNombre: 'Lopez Maria',
      idGenerado: 1003
    },
    {
      idUsuario: 4,
      usuario: 'carlos.gomez',
      contrasena: 'Gomez!456',
      tipoUsuario: 'moderador',
      estado: true,
      idEmpleado: 104,
      apellidoYNombre: 'Gomez Carlos',
      idGenerado: 1004
    }
  ];

}
