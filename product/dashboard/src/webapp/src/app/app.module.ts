import {AgePipe} from './pipes/age.pipe';
import {APP_BASE_HREF, registerLocaleData} from '@angular/common';
import {AppComponent} from './app.component';
import {AppRoutingModule} from './app-routing.module';
import {BrowserModule, Title} from '@angular/platform-browser';
import {CollectionStatusComponent} from './pages/collection-status/collection-status.component';
import {ConfigService} from './services/config.service';
import {CookieModule} from 'ngx-cookie';
import {DashboardApiService} from './services/dashboard-api.service';
import {FormsModule} from '@angular/forms';
import {HomeComponent} from './pages/home/home.component';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';
import {HttpResponseInterceptor} from './services/httpResponse.interceptor';
import {InvocationStatusPipe} from './pipes/invocation-status.pipe';
import {IsLoggedIn} from './guards/is-logged-in';
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

import localeDe from '@angular/common/locales/de';
import localeEn from '@angular/common/locales/en';
import localeEs from '@angular/common/locales/es';
import localeFr from '@angular/common/locales/fr';
import localeSv from '@angular/common/locales/sv';

registerLocaleData(localeDe);
registerLocaleData(localeEn);
registerLocaleData(localeEs);
registerLocaleData(localeFr);
registerLocaleData(localeSv);

function selectBestLocale() {
    let result = window.navigator.language;
    let hyphen = result.indexOf('-');
    if (hyphen > 0) {
        result = result.substr(0, hyphen);
        console.log(`[ck] Stripping variant from window.navigator.language=${window.navigator.language}, using ${result}`);
    }

    const supportedLocales = ['de', 'en', 'es', 'fr', 'sv'];
    if (supportedLocales.indexOf(result) < 0) {
        console.log(`[ck] window.navigator.language=${result}, which is not supported. Falling back to en-US`);
        result = 'en-US';
    }
    console.log('[ck] bestLocale=%o', result);
    return result;
}

@NgModule({
    imports: [
        AppRoutingModule, BrowserModule, CookieModule.forRoot(), FormsModule, HttpClientModule, NgbModule.forRoot(),
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
        ConfigService, IsLoggedIn, StateService, Title, DashboardApiService, {
            provide: APP_BASE_HREF,
            useValue: '/'
        }, {
            provide: LOCALE_ID,
            useValue: selectBestLocale()
        }, {
            provide: HTTP_INTERCEPTORS,
            useClass: HttpResponseInterceptor,
            multi: true,
        }
    ],
    bootstrap: [AppComponent]
})
export class AppModule {
}
