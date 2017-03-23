import {Component} from '@angular/core';
import {WarehouseService} from './warehouse.service';
import {MethodData} from './model/MethodData';
import {CkAgePipe} from './ck-age.pipe';
import {DatePipe} from '@angular/common';

@Component({
    selector: 'ck-methods',
    template: require('./methods.component.html'),
    styles: [require('./methods.component.css')],
    providers: [WarehouseService, CkAgePipe, DatePipe],
})
export class MethodsComponent {

    signature: string;
    maxResults = 100;
    data: MethodData;
    errorMessage: string;
    dateFormat = 'age';

    constructor(private warehouse: WarehouseService) {
    }

    search() {
        this.warehouse
            .getMethods(this.signature, this.maxResults)
            .subscribe(data => {
                this.data = data;
                this.errorMessage = undefined;
            }, error => {
                this.data = undefined;
                this.errorMessage = error;
            }, () => console.log('getMethods() complete'));
    }

}
