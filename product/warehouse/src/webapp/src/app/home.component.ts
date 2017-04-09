import {Component, ViewEncapsulation} from '@angular/core';
@Component({
    selector: 'ck-home',
    template: require('./home.component.html'),
    styles: [require('./home.component.css')],
    encapsulation: ViewEncapsulation.None
})
export class HomeComponent {

    constructor() {
    }

}
