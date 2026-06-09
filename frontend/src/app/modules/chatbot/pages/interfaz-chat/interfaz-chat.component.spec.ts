import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InterfazChatComponent } from './interfaz-chat.component';

describe('InterfazChatComponent', () => {
  let component: InterfazChatComponent;
  let fixture: ComponentFixture<InterfazChatComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InterfazChatComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(InterfazChatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
