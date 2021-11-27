import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import {MethodsComponent} from './methods.component';
import {FormsModule} from '@angular/forms';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {AgePipe} from '../../pipes/age.pipe';
import {Router} from '@angular/router';
import {StateService} from '../../services/state.service';
import {CookieService} from 'ngx-cookie';
import {DashboardApiService} from '../../services/dashboard-api.service';
import {Observable} from 'rxjs';
import {MethodsFormData} from '../../model/methods/methods-form-data';

describe('MethodsComponent', () => {
    let component: MethodsComponent;
    let fixture: ComponentFixture<MethodsComponent>;

    beforeEach(waitForAsync(() => {
        // noinspection JSIgnoredPromiseFromCall
        TestBed.configureTestingModule({
                   imports: [FormsModule],
                   declarations: [MethodsComponent, AgePipe],
                   providers: [
                       {
                           provide: Router,
                           useValue: {} as Router
                       }, {
                           provide: StateService,
                           useValue: new StateService({} as CookieService)
                       }, {
                           provide: DashboardApiService,
                           useValue: {
                               getMethodsFormData() {
                                   return new Observable<MethodsFormData>();
                               }
                           } as DashboardApiService
                       }
                   ],
                   schemas: [NO_ERRORS_SCHEMA],
               })
               .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(MethodsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
