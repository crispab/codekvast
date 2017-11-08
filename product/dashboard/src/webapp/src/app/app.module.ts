import {AgePipe} from './pipes/age.pipe';
import {APP_BASE_HREF, registerLocaleData} from '@angular/common';
import {AppComponent} from './app.component';
import {AppRoutingModule} from './app-routing.module';
import {BrowserModule, Title} from '@angular/platform-browser';
import {CollectionStatusComponent} from './pages/collection-status/collection-status.component';
import {ConfigService} from './services/config.service';
import {FormsModule} from '@angular/forms';
import {HomeComponent} from './pages/home/home.component';
import {HttpModule} from '@angular/http';
import {InvocationStatusPipe} from './pipes/invocation-status.pipe';
import {LOCALE_ID, NgModule} from '@angular/core';
import {LoggedOutComponent} from './pages/auth/logged-out.component';
import {MethodDetailComponent} from './pages/methods/method-detail.component';
import {MethodsComponent} from './pages/methods/methods.component';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {NotLoggedInComponent} from './pages/auth/not-logged-in.component'
import {ReportGeneratorComponent} from './pages/report-generator/report-generator.component';
import {SettingsComponent} from './components/settings-editor.component';
import {SsoComponent} from './components/sso.component';
import {StateService} from './services/state.service';
import {VoteComponent} from './components/vote.component';
import {VoteResultComponent} from './pages/vote-result/vote-result.component';
import {DashboardService} from './services/dashboard.service';
import {AuthTokenRenewer} from './guards/auth-token-renewer';
import {IsLoggedIn} from './guards/is-logged-in';
import localeSv from '@angular/common/locales/sv';

registerLocaleData(localeSv);
console.log('window.navigator.language=%o', window.navigator.language);

@NgModule({
    imports: [
        AppRoutingModule, BrowserModule, FormsModule, HttpModule, NgbModule.forRoot(),
    ],
    declarations: [
        AgePipe,
        AppComponent,
        CollectionStatusComponent,
        HomeComponent,
        InvocationStatusPipe,
        LoggedOutComponent,
        MethodDetailComponent,
        MethodsComponent,
        NotLoggedInComponent,
        ReportGeneratorComponent,
        SettingsComponent,
        SsoComponent,
        VoteComponent,
        VoteResultComponent,
    ],
    providers: [
        AuthTokenRenewer,
        ConfigService,
        IsLoggedIn,
        StateService,
        Title, DashboardService,
        {
            provide: APP_BASE_HREF,
            useValue: '/'
        },
        {
            provide: LOCALE_ID,
            useValue: window.navigator.language
        },
    ],
    bootstrap: [AppComponent]
})
export class AppModule {
}
