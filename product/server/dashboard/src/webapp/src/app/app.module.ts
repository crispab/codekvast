import {AgePipe} from './pipes/age.pipe';
import {APP_BASE_HREF, registerLocaleData} from '@angular/common';
import {AppComponent} from './app.component';
import {AppRoutingModule} from './app-routing.module';
import {BrowserModule, Title} from '@angular/platform-browser';
import {ClipboardModule} from 'ngx-clipboard';
import {CollectionStatusComponent} from './pages/collection-status/collection-status.component';
import {FormsModule} from '@angular/forms';
import {HomeComponent} from './pages/home/home.component';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';
import {InvocationStatusPipe} from './pipes/invocation-status.pipe';
import {LOCALE_ID, NgModule} from '@angular/core';
import {MethodDetailsComponent} from './pages/methods/method-details/method-details.component';
import {MethodsComponent} from './pages/methods/methods.component';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {NotLoggedInComponent} from './pages/auth/not-logged-in/not-logged-in.component';
import {ReportGeneratorComponent} from './pages/report-generator/report-generator.component';
import {SettingsEditorComponent} from './components/settings-editor/settings-editor.component';
import {VoteComponent} from './components/vote/vote.component';
import {VoteResultComponent} from './pages/vote-result/vote-result.component';

import localeDe from '@angular/common/locales/de';
import localeEn from '@angular/common/locales/en';
import localeEs from '@angular/common/locales/es';
import localeFr from '@angular/common/locales/fr';
import localeSv from '@angular/common/locales/sv';
import {IsLoggedIn} from './guards/is-logged-in.guard';
import {ConfigService} from './services/config.service';
import {StateService} from './services/state.service';
import {DashboardApiService} from './services/dashboard-api.service';
import {HttpResponseInterceptor} from './services/httpResponse.interceptor';
import {CookieModule} from 'ngx-cookie';

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
        console.log(`[ck dashboard] Stripping variant from window.navigator.language=${window.navigator.language}, using ${result}`);
    }

    const supportedLocales = ['de', 'en', 'es', 'fr', 'sv'];
    if (supportedLocales.indexOf(result) < 0) {
        console.log(`[ck dashboard] window.navigator.language=${result}, which is not supported. Falling back to en-US`);
        result = 'en-US';
    }
    console.log('[ck dashboard] bestLocale=%o', result);
    return result;
}


@NgModule({
    imports: [
        AppRoutingModule, BrowserModule, ClipboardModule, CookieModule.forRoot(), FormsModule, HttpClientModule, NgbModule
    ],
    declarations: [
        AgePipe,
        AppComponent,
        CollectionStatusComponent,
        InvocationStatusPipe,
        MethodDetailsComponent,
        MethodsComponent,
        NotLoggedInComponent,
        ReportGeneratorComponent,
        SettingsEditorComponent,
        VoteComponent,
        VoteResultComponent,
        HomeComponent,
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
