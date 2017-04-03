import {ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {DebugElement} from '@angular/core';
import {MethodsComponent} from './methods.component';
import {AppModule} from './app.module';
import {WarehouseService} from './warehouse.service';
import {ConfigService} from './config.service';
import {MethodData} from './model/MethodData';
import {Method} from './model/Method';

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

        let warehouseServiceStub = {};

        TestBed.configureTestingModule({
            imports: [AppModule],
            providers: [
                {
                    provide: ConfigService,
                    useValue: {}
                },
                {
                    provide: WarehouseService,
                    useValue: warehouseServiceStub
                }
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
        component.signature = 'New Signature';
        fixture.detectChanges();
        expect(signatureElement.textContent).toBe('New Signature');
    });

    it('should initially sort by signature', () => {
        fixture.detectChanges();
        expect(component.sortColumn).toBe('signature');
    });

    it('should initially sort ascending', () => {
        fixture.detectChanges();
        expect(component.sortAscending).toBe(true);
    });

    it('sortedMethods() should return null when null data', () => {
        expect(component.sortedMethods()).toBeNull();
    });

    it('sortedMethods() should return null when empty data', () => {
        component.data = new MethodData();
        expect(component.sortedMethods()).toBeNull();
    });

    it('sortedMethods() should handle sort by signature ascending', () => {
        component.data = mockData;
        component.sortBySignature();
        component.sortAscending = true;
        expect(component.sortedMethods()[0].signature).toBe('sig1');
    });

    it('sortedMethods() should handle sort by signature descending', () => {
        component.data = mockData;
        component.sortBySignature();
        component.sortAscending = false;
        expect(component.sortedMethods()[0].signature).toBe('sig2');
    });

    it('sortedMethods() should handle sort by age ascending', () => {
        component.data = mockData;
        component.sortByAge();
        component.sortAscending = true;
        expect(component.sortedMethods()[0].lastInvokedAtMillis).toBe(1);
    });

    it('sortedMethods() should handle sort by age descending', () => {
        component.data = mockData;
        component.sortByAge();
        component.sortAscending = false;
        expect(component.sortedMethods()[0].lastInvokedAtMillis).toBe(2);
    });

    it('headerIconClassesSignature() should handle sort by signature ascending', () => {
        component.data = mockData;
        component.sortBySignature()
        component.sortAscending = true;
        expect(component.headerIconClassesSignature()).toEqual({
            'fa': true,
            'fa-sort-asc': true,
            'fa-sort-desc': false,
            'invisible': false
        });
        expect(component.headerIconClassesAge()).toEqual({
            'fa': true,
            'fa-sort-asc': true,
            'fa-sort-desc': false,
            'invisible': true
        });
    });

    it('headerIconClassesAge() should handle sort by age descending', () => {
        component.data = mockData;
        component.sortByAge();
        component.sortAscending = false;
        expect(component.headerIconClassesSignature()).toEqual({
            'fa': true,
            'fa-sort-asc': false,
            'fa-sort-desc': true,
            'invisible': true
        });
        expect(component.headerIconClassesAge()).toEqual({
            'fa': true,
            'fa-sort-asc': false,
            'fa-sort-desc': true,
            'invisible': false
        });
    });

    it('isSelectedMethod() should handle no selected', () => {
        component.selectMethod(null);
        expect(component.isSelectedMethod({id: 1} as Method)).toBeFalsy();
    });

    it('isSelectedMethod() should handle selected method', () => {
        component.selectMethod({id: 1} as Method);
        expect(component.isSelectedMethod({id: 1} as Method)).toBeTruthy();
    });

    it('isSelectedMethod() should handle not selected method', () => {
        component.selectMethod({id: 1} as Method);
        expect(component.isSelectedMethod({id: 2} as Method)).toBeFalsy();
    });

    it('Should prettyPrintAppStatus("EXCLUDED_SINCE_TRIVIAL") as "Excluded since trivial"', () => {
        expect(component.prettyPrintAppStatus('EXCLUDED_SINCE_TRIVIAL')).toBe('Excluded since trivial');
    })
});
