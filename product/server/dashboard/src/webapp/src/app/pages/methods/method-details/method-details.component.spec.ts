import {ActivatedRoute, Params} from '@angular/router';
import {AgePipe} from '../../../pipes/age.pipe';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {Component, NO_ERRORS_SCHEMA} from '@angular/core';
import {InvocationStatusPipe} from '../../../pipes/invocation-status.pipe';
import {MethodDetailsComponent} from './method-details.component';
import {Observable} from 'rxjs';
import {Location} from '@angular/common';
import {StateService} from '../../../services/state.service';
import {CookieService} from 'ngx-cookie';
import {DashboardApiService} from '../../../services/dashboard-api.service';
import {Method} from '../../../model/methods/method';

@Component({
    selector: 'app-settings-editor',
    template: ''
}) // tslint:disable-next-line:component-class-suffix
export class SettingsEditorComponentStub {
};

describe('MethodDetailsComponent', () => {
    let component: MethodDetailsComponent;
    let fixture: ComponentFixture<MethodDetailsComponent>;
    let locationStub: Partial<Location> = {
        back: function () {
        }
    };
    let activatedRouteStub: Partial<ActivatedRoute> = {params: new Observable<Params>()};

    beforeEach(async(() => {
        // noinspection JSIgnoredPromiseFromCall
        TestBed.configureTestingModule({
                   declarations: [MethodDetailsComponent, AgePipe, InvocationStatusPipe, SettingsEditorComponentStub],
                   providers: [
                       {
                           provide: Location,
                           useValue: locationStub
                       }, {
                           provide: StateService,
                           useValue: new StateService({} as CookieService)
                       }, {
                           provide: ActivatedRoute,
                           useValue: activatedRouteStub
                       }, {
                           provide: DashboardApiService,
                           useValue: {
                               getMethodById: function (id: number) {
                                   console.log('getMethodById(%o)', id);
                                   return new Observable<Method>();
                               }
                           } as DashboardApiService
                       }
                   ],
                   schemas: [NO_ERRORS_SCHEMA],
               })
               .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(MethodDetailsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
