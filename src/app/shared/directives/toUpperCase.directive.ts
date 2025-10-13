import { Directive, ElementRef, HostListener, Renderer2 } from '@angular/core';

@Directive({
  selector: '[toUpperCase]',
  standalone: true
})
export class ToUpperCaseDirective {
  constructor(private el: ElementRef, private renderer: Renderer2) {}

  @HostListener('input', ['$event'])
  onInput(event: Event) {
    const input = this.el.nativeElement;
    const upperCaseValue = input.value.toUpperCase();

    this.renderer.setProperty(input, 'value', upperCaseValue);

    // Disparar evento 'input' para actualizar el FormControl en Angular
    const eventInit = new Event('input', { bubbles: true });
    input.dispatchEvent(eventInit);
  }
}
