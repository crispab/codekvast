import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Params} from '@angular/router';
import {DatePipe, Location} from '@angular/common';
import {WarehouseService} from './warehouse.service';
import {Method} from './model/Method';
import {AgePipe} from './age.pipe';
import {StateService} from './state.service';
import {Settings} from './settings';
import {InvocationStatusPipe} from './invocation-status.pipe';

@Component({
    selector: 'ck-method-detail',
    template: require('./method-detail.component.html'),
    styles: [require('./method-detail.component.css')],
    providers: [AgePipe, DatePipe, InvocationStatusPipe]
})
export class MethodDetailComponent implements OnInit {
    method: Method;
    errorMessage: string;
    settings: Settings;

    constructor(private route: ActivatedRoute, private location: Location, private stateService: StateService,
                private warehouse: WarehouseService) {
    }

    ngOnInit(): void {
        this.settings = this.stateService.getState(Settings.KEY, () => new Settings());
        this.route.params
            .switchMap((params: Params) => this.warehouse.getMethodById(+params['id']))
            .subscribe(method => {
                this.method = method;
                this.errorMessage = undefined;
            }, error => {
                console.error('Cannot get method details: %o', error);
                this.method = undefined;
                this.errorMessage = error.statusText ? error.statusText : error;
            }, () => console.log(`getMethodById() complete`));
    }

    goBack(): void {
        this.location.back();
    }

}
