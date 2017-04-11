import {Component, OnInit} from '@angular/core';
import {WarehouseService} from './warehouse.service';
import {AgePipe} from './age.pipe';
import {DatePipe} from '@angular/common';
import {Router} from '@angular/router';
import {StateService} from './state.service';
import {MethodsComponentState} from './methods.component.state';

@Component({
    selector: 'ck-methods',
    template: require('./methods.component.html'),
    styles: [require('./methods.component.css')],
    providers: [AgePipe, DatePipe]
})
export class MethodsComponent implements OnInit {
    static readonly SIGNATURE_COLUMN = 'signature';
    static readonly AGE_COLUMN = 'age';

    state: MethodsComponentState;

    constructor(private router: Router,
                private stateService: StateService,
                private warehouse: WarehouseService,
                private agePipe: AgePipe) {
    }

    ngOnInit(): void {
        this.state = this.stateService.getState('methods', () => new MethodsComponentState(this.warehouse));
    }

    prettyPrintAppStatus(s: string) {
        return s.substr(0, 1).toUpperCase() + s.substr(1).toLowerCase().replace(/_/g, ' ');
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

}
