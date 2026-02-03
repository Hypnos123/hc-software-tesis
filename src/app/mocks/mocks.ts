
export const getLogin = () => {
  return [{
    usuario: {
      apellido: '1',
      idUsuario: 101,
      nombre: 'Usuario',
      tipoUsuario: 'Administrador'
    },

    detallePermisos: [
      {
        nombre: 'Usuarios',
        ruta: '/usuarios',
        idMenu: 1,
        imagen: 'pi pi-user'
      },
      {
        nombre: 'Consultas',
        ruta: '/consultas',
        idMenu: 2,
        imagen: 'pi pi-address-book'
      },
      {
        nombre: 'Historia Clinica',
        ruta: '/historiaClinica',
        idMenu: 3,
        imagen: 'pi pi-file-check'
      },
      {
        nombre: 'Pacientes',
        ruta: '/paciente',
        idMenu: 4,
        imagen: 'pi pi-users'
      },
      {
        nombre: 'Empleados',
        ruta: '/empleados',
        idMenu: 5,
        imagen: 'pi pi-user-edit'
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


export const getConsultas = () => {
  return [
  {
    "id": 3,
    "paciente": "Mendoza Davalos Josefina Vera",
    "dni": "74526981",
    "especialidad": "Medicina General",
    "doctor": "Marcos Chavez",
    "fechaCreacion": "20/11/2024 - 18:00"
  },
  {
    "id": 2,
    "paciente": "Mendoza Davalos Josefina Vera",
    "dni": "74526981",
    "especialidad": "Medicina General",
    "doctor": "Marcos Chavez",
    "fechaCreacion": "20/11/2024 - 18:00"
  },
  {
    "id": 1,
    "paciente": "Mendoza Davalos Josefina Vera",
    "dni": "74526981",
    "especialidad": "Medicina General",
    "doctor": "Marcos Chavez",
    "fechaCreacion": "20/11/2024 - 18:00"
  }
]
}
