import { Directive, HostListener } from '@angular/core';

@Directive({
  selector: '[appOnlyText]',
  standalone: true
})
export class OnlyTextDirective {
  @HostListener('keydown', ['$event'])
  onKeyDown(event: KeyboardEvent) {
    // Permitir teclas de control como Backspace, Tab, Enter, etc.
    if (
      ['Backspace', 'Tab', 'Enter', 'Escape', 'ArrowLeft', 'ArrowRight'].includes(event.key)
    ) {
      return;
    }

    // Evitar números
    if (event.key.match(/[0-9]/)) {
      event.preventDefault();
    }
  }
}
