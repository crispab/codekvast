import {AppModule} from '../../app.module';
import {By} from '@angular/platform-browser';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ConfigService} from '../../services/config.service';
import {DashboardApiService} from '../../services/dashboard-api.service';
import {DebugElement} from '@angular/core';
import {MethodData} from '../../model/methods/MethodData';
import {MethodsComponent} from './methods.component';
import {Method} from '../../model/methods/Method';
import {StateService} from '../../services/state.service';
import {MethodsFormData} from '../../model/methods/MethodsFormData';
import {Subject} from 'rxjs';

let component: MethodsComponent;
let fixture: ComponentFixture<MethodsComponent>;
let signatureDE: DebugElement;
let signatureElement: HTMLElement;

const mockData: MethodData = {
    methods: [
        {
            signature: 'sig2',
            lastInvokedAtMillis: 2
        }, {
            signature: 'sig1',
            lastInvokedAtMillis: 1
        }
    ]
} as MethodData;

describe('MethodsComponent', () => {
    beforeEach(() => {

        let dashboardApiStub = {
            getMethodsFormData() {
                return new Subject<MethodsFormData>();
            }
        };

        TestBed.configureTestingModule({
            imports: [AppModule],
            providers: [
                {
                    provide: ConfigService,
                    useValue: {}
                }, {
                    provide: DashboardApiService,
                    useValue: dashboardApiStub
                }, StateService
            ]
        });

        fixture = TestBed.createComponent(MethodsComponent);

        component = fixture.componentInstance;

        signatureDE = fixture.debugElement.query(By.css('#signature'));
        signatureElement = signatureDE.nativeElement;

    });

    it('should display original signature', () => {
        fixture.detectChanges();
        expect(signatureElement.textContent).toBe('');
    });

    xit('should display a different signature', () => {
        component.state.req.signature = 'New Signature';
        fixture.detectChanges();
        expect(signatureElement.textContent).toBe('New Signature');
    });

    it('should initially sort by signature', () => {
        fixture.detectChanges();
        expect(component.state.sortColumn).toBe('signature');
    });

    it('should initially sort ascending', () => {
        fixture.detectChanges();
        expect(component.state.sortAscending).toBe(true);
    });

    it('sortedMethods() should return null when null data', () => {
        fixture.detectChanges();
        expect(component.state.sortedMethods()).toBeNull();
    });

    it('sortedMethods() should return null when empty data', () => {
        fixture.detectChanges();
        component.state.data = new MethodData();
        expect(component.state.sortedMethods()).toBeNull();
    });

    it('sortedMethods() should handle sort by signature ascending', () => {
        fixture.detectChanges();
        component.state.data = mockData;
        component.state.sortBySignature();
        component.state.sortAscending = true;
        expect(component.state.sortedMethods()[0].signature).toBe('sig1');
    });

    it('sortedMethods() should handle sort by signature descending', () => {
        fixture.detectChanges();
        component.state.data = mockData;
        component.state.sortBySignature();
        component.state.sortAscending = false;
        expect(component.state.sortedMethods()[0].signature).toBe('sig2');
    });

    it('sortedMethods() should handle sort by age ascending', () => {
        fixture.detectChanges();
        component.state.data = mockData;
        component.state.sortByAge();
        component.state.sortAscending = true;
        expect(component.state.sortedMethods()[0].lastInvokedAtMillis).toBe(1);
    });

    it('sortedMethods() should handle sort by age descending', () => {
        fixture.detectChanges();
        component.state.data = mockData;
        component.state.sortByAge();
        component.state.sortAscending = false;
        expect(component.state.sortedMethods()[0].lastInvokedAtMillis).toBe(2);
    });

    it('headerIconClassesSignature() should handle sort by signature ascending', () => {
        fixture.detectChanges();
        component.state.data = mockData;
        component.state.sortBySignature();
        component.state.sortAscending = true;
        expect(component.state.headerIconClassesSignature()).toEqual({
            'fas': true,
            'fa-sort-down': true,
            'fa-sort-up': false,
            'invisible': false
        });
        expect(component.state.headerIconClassesAge()).toEqual({
            'fas': true,
            'fa-sort-down': true,
            'fa-sort-up': false,
            'invisible': true
        });
    });

    it('headerIconClassesAge() should handle sort by age descending', () => {
        fixture.detectChanges();
        component.state.data = mockData;
        component.state.sortByAge();
        component.state.sortAscending = false;
        expect(component.state.headerIconClassesSignature()).toEqual({
            'fas': true,
            'fa-sort-down': false,
            'fa-sort-up': true,
            'invisible': true
        });
        expect(component.state.headerIconClassesAge()).toEqual({
            'fas': true,
            'fa-sort-down': false,
            'fa-sort-up': true,
            'invisible': false
        });
    });

    it('isSelectedMethod() should handle no selected', () => {
        fixture.detectChanges();
        component.state.selectMethod(null);
        expect(component.state.isSelectedMethod({id: 1} as Method)).toBeFalsy();
    });

    it('isSelectedMethod() should handle selected method', () => {
        fixture.detectChanges();
        component.state.selectMethod({id: 1} as Method);
        expect(component.state.isSelectedMethod({id: 1} as Method)).toBeTruthy();
    });

    it('isSelectedMethod() should handle not selected method', () => {
        fixture.detectChanges();
        component.state.selectMethod({id: 1} as Method);
        expect(component.state.isSelectedMethod({id: 2} as Method)).toBeFalsy();
    });

});
