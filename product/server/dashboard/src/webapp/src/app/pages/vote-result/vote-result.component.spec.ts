import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import {VoteResultComponent} from './vote-result.component';
import {Location} from '@angular/common';
import {ActivatedRoute, Params} from '@angular/router';
import {Observable} from 'rxjs';

describe('VoteResultComponent', () => {
    let component: VoteResultComponent;
    let fixture: ComponentFixture<VoteResultComponent>;

    const locationStub: Partial<Location> = {back() {}};
    const activatedRouteStub: Partial<ActivatedRoute> = {params: new Observable<Params>()};

    beforeEach(waitForAsync(() => {
        // noinspection JSIgnoredPromiseFromCall
        TestBed.configureTestingModule({
                   declarations: [VoteResultComponent],
                   providers: [
                       {provide: Location, useValue: locationStub},
                       {provide: ActivatedRoute, useValue: activatedRouteStub}
                   ]
               })
               .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(VoteResultComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
