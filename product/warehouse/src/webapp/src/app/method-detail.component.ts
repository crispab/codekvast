import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Params} from '@angular/router';
import {DatePipe, Location} from '@angular/common';
import {WarehouseService} from './warehouse.service';
import {AgePipe} from './age.pipe';
import {Method} from './model/Method';

@Component({
    selector: 'ck-method-detail',
    template: require('./method-detail.component.html'),
    styles: [require('./method-detail.component.css')],
    providers: [WarehouseService, AgePipe, DatePipe],
})
export class MethodDetailComponent implements OnInit {
    method: Method;

    constructor(private route: ActivatedRoute, private location: Location, private warehouse: WarehouseService) {
    }

    ngOnInit(): void {
        this.route.params
            .switchMap((params: Params) => this.warehouse.getMethodById(+params['id']))
            .subscribe(method => this.method = method);
    }

    goBack(): void {
        this.location.back();
    }

}
