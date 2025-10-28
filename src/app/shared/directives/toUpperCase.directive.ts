import { Directive, ElementRef, HostListener, Renderer2 } from '@angular/core';

@Directive({
  selector: '[toUpperCase]',
  standalone: true
})
export class ToUpperCaseDirective {
  constructor(private el: ElementRef, private renderer: Renderer2) {}

  @HostListener('input', ['$event'])
  onInput(event: Event) {
    const input = this.el.nativeElement as HTMLInputElement;
    const currentValue = input.value;
    const upperCaseValue = currentValue.toUpperCase();

    // Solo actualiza si realmente hay un cambio
    if (currentValue !== upperCaseValue) {
      this.renderer.setProperty(input, 'value', upperCaseValue);

      // Esto actualizará el FormControl solo una vez
      const eventInit = new Event('input', { bubbles: true });
      input.dispatchEvent(eventInit);
    }
  }
}
