import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import {NotLoggedInComponent} from './not-logged-in.component';
import {ConfigService} from '../../../services/config.service';

describe('NotLoggedInComponent', () => {
    let component: NotLoggedInComponent;
    let fixture: ComponentFixture<NotLoggedInComponent>;

    beforeEach(waitForAsync(() => {
        // noinspection JSIgnoredPromiseFromCall
        TestBed.configureTestingModule({
                   declarations: [NotLoggedInComponent],
                   providers: [
                       {
                           provide: ConfigService,
                           useValue: new ConfigService()
                       }
                   ]
               })
               .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(NotLoggedInComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
