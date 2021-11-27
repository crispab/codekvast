import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import {CollectionStatusComponent} from './collection-status.component';
import {Component, Input, NO_ERRORS_SCHEMA} from '@angular/core';
import {AgePipe} from '../../pipes/age.pipe';
import {StateService} from '../../services/state.service';
import {DashboardApiService} from '../../services/dashboard-api.service';
import {CookieService} from 'ngx-cookie';
import {Observable} from 'rxjs';
import {StatusData} from '../../model/status/status-data';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'ngb-progressbar',
    template: ''
})
export class NgbProgressbarStubComponent {
    @Input() showValue: boolean;
    @Input() type: any;
    @Input() value: any;
};

@Component({
    selector: 'app-settings-editor',
    template: ''
})
// eslint-disable-next-line @angular-eslint/component-class-suffix
export class SettingsEditorComponentStub {
};

describe('CollectionStatusComponent', () => {
    let component: CollectionStatusComponent;
    let fixture: ComponentFixture<CollectionStatusComponent>;
    beforeEach(waitForAsync(() => {
        // noinspection JSIgnoredPromiseFromCall
        TestBed.configureTestingModule({
                   declarations: [CollectionStatusComponent, NgbProgressbarStubComponent, AgePipe, SettingsEditorComponentStub],
                   providers: [
                       {
                           provide: StateService,
                           useValue: new StateService({} as CookieService)
                       }, {
                           provide: DashboardApiService,
                           useValue: {
                               getStatus() {
                                   return new Observable<StatusData>();
                               }
                           } as DashboardApiService
                       }
                   ],
                   schemas: [NO_ERRORS_SCHEMA]
               })
               .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(CollectionStatusComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
