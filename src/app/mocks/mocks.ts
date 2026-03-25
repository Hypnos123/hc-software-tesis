
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
      "idUsuario": 1,
      "usuario": "jgomez",
      "contrasena": "hashed_abc123",
      "tipoUsuario": "ADMINISTRADOR",
      "estado": true,
      "idEmpleado": 101,
      "apellidoYNombre": "Gómez, Juan Carlos"
    },
    {
      "idUsuario": 2,
      "usuario": "mlopez",
      "contrasena": "hashed_xyz789",
      "tipoUsuario": "DOCTOR",
      "estado": true,
      "idEmpleado": 102,
      "apellidoYNombre": "López, María Elena"
    },
    {
      "idUsuario": 3,
      "usuario": "rperez",
      "contrasena": "hashed_qrs456",
      "tipoUsuario": "ENFERMERO",
      "estado": false,
      "idEmpleado": 103,
      "apellidoYNombre": "Pérez, Roberto Andrés"
    },
    {
      "idUsuario": 4,
      "usuario": "amorales",
      "contrasena": "hashed_lmn321",
      "tipoUsuario": "DOCTOR",
      "estado": true,
      "idEmpleado": 104,
      "apellidoYNombre": "Morales, Ana Lucía"
    }
  ]
}

export const getByIdUsuario = (id: number) => {
  const usuarios = getAllUsuarios();
  return usuarios.find(usuario => usuario.idUsuario === id);
}

export const getPacientes = () => {

return  [
  {
    idAntecedentes: 1,
    alimentacion: 'Balanceada, consumo frecuente de frutas y verduras',
    habitos: 'No fuma, consumo ocasional de alcohol',
    vivienda: 'Casa propia con servicios básicos',
    desarrolloPsicomotor: 'Adecuado para la edad',
    vacunas: 'Esquema completo',
    educacion: 'Secundaria completa',
    cirugiasPrevias: 'Apendicectomía en 2015',
    alergiaMedicamentos: 'Penicilina',
    idPaciente: 101,
    nombreApellidos: 'Juan Pérez García'
  },
  {
    idAntecedentes: 2,
    alimentacion: 'Dieta alta en carbohidratos',
    habitos: 'Fumador ocasional',
    vivienda: 'Departamento alquilado',
    desarrolloPsicomotor: 'Normal',
    vacunas: 'Esquema incompleto',
    educacion: 'Técnico superior',
    cirugiasPrevias: 'Ninguna',
    alergiaMedicamentos: 'Ninguna',
    idPaciente: 102,
    nombreApellidos: 'María López Sánchez'
  },
  {
    idAntecedentes: 3,
    alimentacion: 'Vegetariana',
    habitos: 'Actividad física regular',
    vivienda: 'Casa familiar',
    desarrolloPsicomotor: 'Adecuado',
    vacunas: 'Esquema completo',
    educacion: 'Universitaria',
    cirugiasPrevias: 'Cesárea en 2020',
    alergiaMedicamentos: 'Ibuprofeno',
    idPaciente: 103,
    nombreApellidos: 'Carlos Ramírez Torres'
  }
]
}

export const getAllEmpleados = () => {
  return [
    {
      "idEmpleado": 101,
      "tipoDocumento": "DNI",
      "numDocumento": "45123678",
      "nombre": "Juan Carlos",
      "apellido": "Gómez",
      "direccion": "Av. Larco 123, Miraflores",
      "telefono": "014512367",
      "celular": "987654321",
      "cargo": "Gerente General",
      "estado": true
    },
    {
      "idEmpleado": 102,
      "tipoDocumento": "DNI",
      "numDocumento": "47891234",
      "nombre": "María Elena",
      "apellido": "López",
      "direccion": "Jr. Cusco 456, San Isidro",
      "telefono": "014789123",
      "celular": "976543210",
      "cargo": "Analista de Sistemas",
      "estado": true
    },
    {
      "idEmpleado": 103,
      "tipoDocumento": "CE",
      "numDocumento": "001234567",
      "nombre": "Roberto Andrés",
      "apellido": "Pérez",
      "direccion": "Calle Las Flores 89, Surco",
      "telefono": "016789012",
      "celular": "965432109",
      "cargo": "Contador",
      "estado": false
    },
    {
      "idEmpleado": 104,
      "tipoDocumento": "DNI",
      "numDocumento": "43567890",
      "nombre": "Ana Lucía",
      "apellido": "Morales",
      "direccion": "Av. Javier Prado 789, San Borja",
      "telefono": "013456789",
      "celular": "954321098",
      "cargo": "Recursos Humanos",
      "estado": true
    },
    {
      "idEmpleado": 105,
      "tipoDocumento": "DNI",
      "numDocumento": "46234567",
      "nombre": "Carlos",
      "apellido": "Quispe",
      "direccion": "Jr. Tacna 321, Cercado",
      "telefono": "012345678",
      "celular": "943210987",
      "cargo": "Almacenero",
      "estado": true
    }
  ]
}

export const getAllMenus = () => {
  return [

      {
        nombre: 'Usuarios',
        ruta: '/usuarios',
        idMenu: 1,
        imagen: 'pi pi-user',
        estado: true
      },
      {
        nombre: 'Consultas',
        ruta: '/consultas',
        idMenu: 2,
        imagen: 'pi pi-address-book',
        estado: true
      },
      {
        nombre: 'Historia Clinica',
        ruta: '/historiaClinica',
        idMenu: 3,
        imagen: 'pi pi-file-check',
        estado: true
      },
      {
        nombre: 'Pacientes',
        ruta: '/paciente',
        idMenu: 4,
        imagen: 'pi pi-users',
        estado: true
      },
      {
        nombre: 'Empleados',
        ruta: '/empleados',
        idMenu: 5,
        imagen: 'pi pi-user-edit',
        estado: true
      },
  ]
}

export const getByIdDetallePermiso = (idUsuario: number) => {
  return [
    {
      idUsuario: 1,
      idMenu: 1
    },
    {
      idUsuario: 1,
      idMenu: 2
    }
  ]
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