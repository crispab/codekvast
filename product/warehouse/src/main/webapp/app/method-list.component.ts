import {Component} from 'angular2/core';
import {WarehouseService} from './warehouse.service';
import {MethodData} from './model/MethodData';

@Component({
    selector: 'ck-method-list', templateUrl: 'app/method-list.component.html', providers: [WarehouseService]
})
export class MethodListComponent {

    signature: string;
    maxResults: number = 100;
    data: MethodData;
    errorMessage: string;

    constructor(private warehouse: WarehouseService) {
    }

    search() {
        this.warehouse.getMethods(this.signature, this.maxResults).subscribe(
            (data) => this.data = data,
            (error) => this.errorMessage = error);
    }

    get diagnostic() {
        return this.data ? JSON.stringify(this.data) : undefined;
    }
}
