import { AUDITORIA_ROUTES } from './auditoria.routes';

describe('AUDITORIA_ROUTES', () => {
  const children = AUDITORIA_ROUTES[0].children ?? [];

  it('redirige la raíz a pacientes archivados', () => {
    expect(children.find((route) => route.path === '')?.redirectTo).toBe('pacientes-archivados');
  });

  it('declara las dos rutas hijas', () => {
    expect(children.some((route) => route.path === 'pacientes-archivados')).toBeTrue();
    expect(children.some((route) => route.path === 'fusiones-historias')).toBeTrue();
  });
});
