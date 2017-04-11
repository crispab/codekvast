import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Params} from '@angular/router';
import {Location} from '@angular/common';
import {WarehouseService} from './warehouse.service';
import {Method} from './model/Method';

@Component({
    selector: 'ck-method-detail',
    template: require('./method-detail.component.html'),
    styles: [require('./method-detail.component.css')]
})
export class MethodDetailComponent implements OnInit {
    method: Method;
    errorMessage: string;

    constructor(private route: ActivatedRoute, private location: Location, private warehouse: WarehouseService) {
    }

    ngOnInit(): void {
        this.route.params
            .switchMap((params: Params) => this.warehouse.getMethodById(+params['id']))
            .subscribe(method => {
                this.method = method;
                this.errorMessage = undefined;
            }, error => {
                this.method = undefined;
                this.errorMessage = error;
            }, () => console.log(`getMethodById() complete`));
    }

    goBack(): void {
        this.location.back();
    }

}
