import {Component} from '@angular/core';
import {WarehouseService} from './warehouse.service';
import {MethodData} from './model/MethodData';
import {CkAgePipe} from './ck-age.pipe';

@Component({
    selector: 'ck-method-list',
    template: require('./method-list.component.html'),
    providers: [WarehouseService, CkAgePipe],
})
export class MethodListComponent {

    signature: string;
    maxResults: number = 100;
    data: MethodData;
    errorMessage: string;
    dateFormat = 'age';

    constructor(private warehouse: WarehouseService) {
    }

    search() {
        this.warehouse
            .getMethods(this.signature, this.maxResults)
            .subscribe((data) => this.data = data, (error) => this.errorMessage = error);
    }

}
