import {Component} from "angular2/core";

@Component({
    selector: 'codekvast-warehouse',
    template: '<h1>Codekvast Warehouse</h1>' +
    '<p>Page loaded at {{ now }}</p>'
})
export class AppComponent {

    private now:Date = new Date()

}
