import {APP_BASE_HREF, registerLocaleData} from '@angular/common';
import {AppComponent} from './app.component';
import {AppRoutingModule} from './app-routing.module';
import {BrowserModule, Title} from '@angular/platform-browser';
import {ConfigService} from './services/Config.service';
import {CookieModule} from 'ngx-cookie';
import {ForbiddenComponent} from './pages/forbidden.component';
import {FormsModule} from '@angular/forms';
import {HomeComponent} from './pages/home.component';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';
import {HttpResponseInterceptor} from './services/HttpResponse.interceptor';
import {LOCALE_ID, NgModule} from '@angular/core';
import {LoginApiService} from './services/LoginApi.service';
import {LoginComponent} from './pages/login.component';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {StartComponent} from './pages/start.component';

import localeDe from '@angular/common/locales/de';
import localeEn from '@angular/common/locales/en';
import localeEs from '@angular/common/locales/es';
import localeFr from '@angular/common/locales/fr';
import localeSv from '@angular/common/locales/sv';
import {IsAuthenticated} from './guards/isAuthenticated';

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
        AppComponent, ForbiddenComponent, HomeComponent, LoginComponent, StartComponent,
    ],
    providers: [
        ConfigService, IsAuthenticated, Title, LoginApiService, {
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
