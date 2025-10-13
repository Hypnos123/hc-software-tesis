import { Directive, ElementRef, Input, OnInit } from '@angular/core';

@Directive({
  selector: '[appStyleButton]',
  standalone: true
})
export class StyleButtonDirective implements OnInit {

  @Input() clase: string = '';

  constructor(private el: ElementRef) { }

  ngOnInit() {
    let clases: string[] = [];
    this.clase.split(' ').map((x) => clases.push(x));


    switch (clases.length) {
      case 2:
        this.el.nativeElement.classList.add(clases[0], clases[1]);
        break;

      case 3:
        this.el.nativeElement.classList.add(clases[0], clases[1], clases[2]);
        break;

      default:
        this.el.nativeElement.classList.add(this.clase);
        break;
    }
  }
}

