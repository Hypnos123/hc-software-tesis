import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NavbarTemplateComponent } from './navbar-template.component';

describe('NavbarTemplateComponent', () => {
  let component: NavbarTemplateComponent;
  let fixture: ComponentFixture<NavbarTemplateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ NavbarTemplateComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(NavbarTemplateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
