import {Component, OnInit} from '@angular/core';
import {WarehouseService} from '../../services/warehouse.service';
import {AgePipe} from '../../pipes/age.pipe';
import {DatePipe} from '@angular/common';
import {Router} from '@angular/router';
import {StateService} from '../../services/state.service';
import {MethodsComponentState} from './methods.component.state';
import {Settings} from '../../components/settings.model';
import {Method} from '../../model/methods/Method';

@Component({
    selector: 'ck-methods',
    template: require('./methods.component.html'),
    styles: [require('./methods.component.css')],
    providers: [AgePipe, DatePipe]
})
export class MethodsComponent implements OnInit {
    static readonly SIGNATURE_COLUMN = 'signature';
    static readonly AGE_COLUMN = 'age';

    settings: Settings;
    state: MethodsComponentState;

    constructor(private router: Router, private stateService: StateService,
                private warehouse: WarehouseService, private agePipe: AgePipe) {
    }

    ngOnInit(): void {
        this.settings = this.stateService.getState(Settings.KEY, () => new Settings());
        this.state = this.stateService.getState(MethodsComponentState.KEY, () => new MethodsComponentState(this.warehouse));
    }

    communicationFailure() {
        let now = this.agePipe.transform(new Date(), 'shortTime');
        return now + ': Communication failure'
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
