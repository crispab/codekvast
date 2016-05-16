import {Component} from 'angular2/core';
import {WarehouseService} from './warehouse.service';

@Component({
    selector: 'ck-method', templateUrl: 'app/method.component.html', providers: [WarehouseService]
})
export class MethodComponent {

    signature: string;
    maxResults: number = 100;
    active = true;
    methods: any[] = [];
    errorMessage: string;

    constructor(private warehouse: WarehouseService) {
    }

    search() {
        this.active = false;
        this.warehouse.getMethods(this.signature, this.maxResults).subscribe(
            (data) => this.methods = data,
            (error) => this.errorMessage = error);
        setTimeout(()=> this.active = true, 0);
    }
}
