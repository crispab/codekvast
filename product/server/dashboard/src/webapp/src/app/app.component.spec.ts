import {AppComponent} from './app.component';
import { TestBed, waitForAsync } from '@angular/core/testing';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {RouterTestingModule} from '@angular/router/testing';
import {ConfigService} from './services/config.service';
import {DashboardApiService} from './services/dashboard-api.service';
import {CookieService} from 'ngx-cookie';
import {StateService} from './services/state.service';
import {from} from 'rxjs';

describe('AppComponent', () => {
    let fakeApiService = {getServerSettings: () => from([{serverVersion: 'some-server-version'}])};
    let fakeCookieService = {get: () => ''};

    beforeEach(waitForAsync(() => {

        TestBed.configureTestingModule({
            imports: [
                RouterTestingModule, NgbModule
            ],
            providers: [
                ConfigService, StateService, {
                    provide: CookieService,
                    useValue: fakeCookieService
                }, {
                    provide: DashboardApiService,
                    useValue: fakeApiService
                }
            ],
            declarations: [
                AppComponent
            ],
        }).compileComponents();
  }));

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render title in a title tag', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.debugElement.nativeElement;
    expect(compiled.querySelector('h1#title').textContent).toContain('Codekvast');
  });

});
