import {ActivatedRoute, Params} from '@angular/router';
import {AgePipe} from '../../pipes/age.pipe';
import {Component, OnInit} from '@angular/core';
import {DashboardApiService} from '../../services/dashboard-api.service';
import {DatePipe, Location} from '@angular/common';
import {InvocationStatusPipe} from '../../pipes/invocation-status.pipe';
import {Method} from '../../model/methods/Method';
import {Settings} from '../../components/settings.model';
import {StateService} from '../../services/state.service';

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
                private api: DashboardApiService, private agePipe: AgePipe) {
    }

    ngOnInit(): void {
        this.settings = this.stateService.getState(Settings.KEY, () => new Settings());
        this.route.params
            .switchMap((params: Params) => this.api.getMethodById(+params['id']))
            .subscribe(method => {
                this.method = method;
                this.errorMessage = undefined;
            }, error => {
                console.error('Cannot get method details: %o', error);
                this.method = undefined;
                this.errorMessage = error.statusText ? error.statusText : error;
            });
    }

    goBack(): void {
        this.location.back();
    }

    hasInconsistentTracking() {
        return Method.hasInconsistentTracking(this.method);
    }

    communicationFailure() {
        let now = this.agePipe.transform(new Date(), this.settings.dateFormat);
        return now + ': Communication failure'
    }
}
