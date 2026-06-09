
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
    idPaciente: 101,
    nombreApellidos: 'Juan Pérez García',
    edad: "28",
    dni: "74589632",
    fechaRegistro: "12/05/2026",
    apellidos: "Garcia",
    nombres: "Juan Perez",
    fechaIngreso: "12/05/2026",
    fechaNacimiento: "02/12/1999",
    estadoCivil: "Soltero",
    sexo: "Masculino",
    direccion: "Mi casa",
    distrito: "Cerca de mi casa",
    traidoPor: "Ninguno",
    alimentacion: 'Dieta alta en carbohidratos',
    habitos: 'Fumador ocasional',
    vivienda: 'Departamento alquilado',
    desarrolloPsicomotor: 'Normal',
    vacunas: 'Esquema incompleto',
    educacion: 'Tecnico',
    enfermedadesPrevias: "Asma",
    cirugiasPrevias: 'Ninguna',
    alergiaMedicamentos: 'Ninguna'
  },
  {
  idPaciente: 102,
  nombreApellidos: 'María López Sánchez',
  edad: "34",
  dni: "81234567",
  fechaRegistro: "20/06/2026",
  apellidos: "López Sánchez",
  nombres: "María Fernanda",
  fechaIngreso: "20/06/2026",
  fechaNacimiento: "11/08/1991",
  estadoCivil: "Casada",
  sexo: "Femenino",
  direccion: "Av. Los Olivos 123",
  distrito: "Los Olivos",
  traidoPor: "Esposo",
  alimentacion: 'Dieta balanceada',
  habitos: 'No fuma, consumo ocasional de alcohol',
  vivienda: 'Casa propia',
  desarrolloPsicomotor: 'Normal',
  vacunas: 'Esquema completo',
  educacion: 'Superior',
  enfermedadesPrevias: "Hipertensión",
  cirugiasPrevias: 'Cesárea',
  alergiaMedicamentos: 'Penicilina'
  },
  {
  idPaciente: 103,
  nombreApellidos: 'Carlos Ramírez Torres',
  edad: "45",
  dni: "69874521",
  fechaRegistro: "05/07/2026",
  apellidos: "Ramírez Torres",
  nombres: "Carlos Alberto",
  fechaIngreso: "05/07/2026",
  fechaNacimiento: "10/03/1981",
  estadoCivil: "Divorciado",
  sexo: "Masculino",
  direccion: "Jr. San Martín 456",
  distrito: "San Juan de Lurigancho",
  traidoPor: "Hijo",
  alimentacion: 'Dieta baja en sal',
  habitos: 'Sedentario',
  vivienda: 'Departamento propio',
  desarrolloPsicomotor: 'Normal',
  vacunas: 'Esquema completo',
  educacion: 'Secundaria',
  enfermedadesPrevias: "Diabetes tipo 2",
  cirugiasPrevias: 'Apendicectomía',
  alergiaMedicamentos: 'Ninguna'
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
      "cargo": "Doctor",
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
      "cargo": "Enfermera(o)",
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
      "cargo": "Administrador",
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
      "cargo": "Administrador",
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
      "cargo": "Administrador",
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
    "paciente": "Ramírez Torres Luis Alberto",
    "dni": "73214589",
    "especialidad": "Cardiología",
    "doctor": "Patricia Ríos",
    "fechaCreacion": "15/03/2025 - 10:30"
  },
  {
    "id": 1,
    "paciente": "García López Andrea Milagros",
    "dni": "78965412",
    "especialidad": "Pediatría",
    "doctor": "José Valdivia",
    "fechaCreacion": "12/02/2025 - 09:15"
  }

 ]
}



export const getDetalleConsultas = () => {
  return [
    {
      id: 1,
      nombreCompleto: 'Herrera Muñoz Juan Pablo',
      dni: '74589632',
      edad: 41,

      enfermedadesPrevias: 'Gastritis',
      cirugiasPrevias: 'Ninguna',
      alergiasMedicamentos: 'Ninguna',

      presion: '120/80',
      frecuenciaCardiaca: '78',
      frecuenciaRespiratoria: '18',
      talla: '1.70',
      temperatura: '36.7',
      peso: '72',

      fechaConsulta: '20/11/2024',
      tiempoEnfermedad: '2 días',
      tipoEnfermedad: 'Aguda',
      relato: 'Paciente refiere dolor abdominal desde hace dos días.',

      diagnostico: '',
      examenesRecetados: '',
      receta: '',
      tratamiento: '',
      proximaCita: ''
    },
    {
      id: 3,
      nombreCompleto: 'Mendoza Davalos Josefina Vera',
      dni: '74526981',
      edad: 41,

      enfermedadesPrevias: 'Hipertensión',
      cirugiasPrevias: 'Apendicectomía',
      alergiasMedicamentos: 'Penicilina',

      presion: '120/80',
      frecuenciaCardiaca: '78',
      frecuenciaRespiratoria: '18',
      talla: '1.62',
      temperatura: '36.7',
      peso: '65',

      fechaConsulta: '10/05/2026',
      tiempoEnfermedad: '3 días',
      tipoEnfermedad: 'Aguda',
      relato: 'Paciente refiere dolor abdominal moderado desde hace tres días.',

      diagnostico: 'Dolor abdominal en evaluación',
      examenesRecetados: 'Hemograma completo y examen de orina',
      receta: 'Paracetamol 500mg cada 8 horas por 3 días',
      tratamiento: 'Reposo, hidratación y dieta blanda',
      proximaCita: '17/05/2026'
    }
  ];
};





export const getHistoriasClinicas = () => {
  return [
    {
      idHistoriaClinica: 1,
      nombrePacienteSel: 'Mendoza Davalos Josefina Vera',
      dniSel: '74526981',
      fechaIngreso: '25/03/2026',
      apellidos: 'Mendoza Davalos',
      nombres: 'Josefina Vera',
      estadoCivil: 'SOLTERO',
      edad: 41,
      dni: '74526981',

      alimentacion: 'Dieta balanceada',
      habitos: 'No fuma, consumo ocasional de café',
      vivienda: 'Casa propia',
      desarrolloPsicomotor: 'Normal',
      vacunas: 'Esquema completo',
      educacion: 'T',

      enfPrevias: 'Hipertensión',
      cirugiasPrevias: 'Apendicectomía',
      alergiasMedicamentos: 'Penicilina'
    },
    {
      idHistoriaClinica: 2,
      nombrePacienteSel: 'Quispe Huamán Luis Alberto',
      dniSel: '42831659',
      fechaIngreso: '18/03/2026',
      apellidos: 'Quispe Huamán',
      nombres: 'Luis Alberto',
      estadoCivil: 'SOLTERO',
      edad: 29,
      dni: '42831659',

      alimentacion: 'Dieta alta en carbohidratos',
      habitos: 'Fumador ocasional',
      vivienda: 'Departamento alquilado',
      desarrolloPsicomotor: 'Normal',
      vacunas: 'Esquema incompleto',
      educacion: 'S1',

      enfPrevias: 'Asma',
      cirugiasPrevias: 'Ninguna',
      alergiasMedicamentos: 'Ninguna'
    },
    {
      idHistoriaClinica: 3,
      nombrePacienteSel: 'Rojas Salazar María Elena',
      dniSel: '76351844',
      fechaIngreso: '10/03/2026',
      apellidos: 'Rojas Salazar',
      nombres: 'María Elena',
      estadoCivil: 'CASADO',
      edad: 36,
      dni: '76351844',
      alimentacion: 'Dieta baja en azúcar',
      habitos: 'Sedentaria',
      vivienda: 'Casa familiar',
      desarrolloPsicomotor: 'Normal',
      vacunas: 'Esquema completo',
      educacion: 'S1',
      enfPrevias: 'Diabetes tipo 2',
      cirugiasPrevias: 'Cesárea',
      alergiasMedicamentos: 'Ibuprofeno'
    }
  ];
};


