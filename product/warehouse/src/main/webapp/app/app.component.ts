import {Component} from 'angular2/core';
import {window} from 'angular2/src/facade/browser';

@Component({
    selector: 'codekvast-warehouse',
    templateUrl: 'templates/app.component.html'
})
export class AppComponent {

    api: String;
    now:Date = new Date();

    constructor() {
        this.api = window['CODEKVAST_API'];
    }

}
