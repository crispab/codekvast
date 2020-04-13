import {ActivatedRoute, Params} from '@angular/router';
import {AgePipe} from '../../../pipes/age.pipe';
import {ClientSettings} from '../../../model/client-settings';
import {ClipboardService} from 'ngx-clipboard';
import {Component, OnInit} from '@angular/core';
import {DashboardApiService} from '../../../services/dashboard-api.service';
import {DatePipe, Location} from '@angular/common';
import {InvocationStatusPipe} from '../../../pipes/invocation-status.pipe';
import {Method} from '../../../model/methods/method';
import {StateService} from '../../../services/state.service';
import {switchMap} from 'rxjs/operators';
import {MethodsComponent} from '../methods.component';

@Component({
    selector: 'app-method-details',
    templateUrl: './method-details.component.html',
    providers: [AgePipe, DatePipe, InvocationStatusPipe]
})
export class MethodDetailsComponent implements OnInit {

    method: Method;
    errorMessage: string;
    settings: ClientSettings;

    constructor(private route: ActivatedRoute, private location: Location, private stateService: StateService,
                private api: DashboardApiService, private agePipe: AgePipe, private clipboardService: ClipboardService) {
    }

    ngOnInit(): void {
        this.settings = this.stateService.getState(ClientSettings.KEY, () => new ClientSettings());
        this.route.params.pipe(switchMap((params: Params) => this.api.getMethodById(+params['id'])))
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
        return now + ': Communication failure';
    }

    copySignatureToClipboard() {
        this.clipboardService.copyFromContent(Method.stripArgumentsFromSignature(this.method));
    }

    probablyGoneClasses() {
        let probablyGone = Method.isProbablyGone(this.method, MethodsComponent.PROBABLY_GONE_DAYS);
        return {
            invisible: !probablyGone,
            rounded: probablyGone,
            'bg-success': probablyGone,
            'text-white': probablyGone,
            'font-italic': probablyGone,
            'p-1': probablyGone,
            'ml-3': probablyGone
        };
    }

    getProbablyGoneDays() {
        return MethodsComponent.PROBABLY_GONE_DAYS;
    }

}
