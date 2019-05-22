import {AgePipe} from '../../pipes/age.pipe';
import {ClientSettings} from '../../model/client-settings';
import {Component, OnInit} from '@angular/core';
import {DashboardApiService} from '../../services/dashboard-api.service';
import {DatePipe} from '@angular/common';
import {MethodsComponentState} from './methods.component.state';
import {Method} from '../../model/methods/method';
import {Router} from '@angular/router';
import {StateService} from '../../services/state.service';

@Component({
    selector: 'app-methods',
    templateUrl: './methods.component.html',
    styleUrls: ['./methods.component.scss'],
    providers: [AgePipe, DatePipe]
})
export class MethodsComponent implements OnInit {
    static readonly SIGNATURE_COLUMN = 'signature';
    static readonly AGE_COLUMN = 'age';
    static readonly COLLECTED_DAYS_COLUMN = 'collectedDays';

    settings: ClientSettings;
    state: MethodsComponentState;

    constructor(private router: Router, private stateService: StateService, private api: DashboardApiService, private agePipe: AgePipe) {
    }

    ngOnInit(): void {
        this.settings = this.stateService.getState(ClientSettings.KEY, () => new ClientSettings());
        this.state = this.stateService.getState(MethodsComponentState.KEY, () => new MethodsComponentState(this.api));
        this.state.initialize();
    }

    communicationFailure() {
        let now = this.agePipe.transform(new Date(), this.settings.dateFormat);
        return now + ': Communication failure';
    }

    gotoMethodDetail(id: number): void {
        if (this.state.selectedMethod) {
            //noinspection JSIgnoredPromiseFromCall
            this.router.navigate(['/method', id]);
        }
    }

    signatureClasses(method: Method) {
        let muted = method.trackedPercent < 100;
        return {
            'text-muted': muted,
            'font-italic': muted,
        };
    }

    hasInconsistentTracking(m: Method) {
        return Method.hasInconsistentTracking(m);
    }
}
