import {APP_BASE_HREF, registerLocaleData} from '@angular/common';
import {AppComponent} from './app.component';
import {AppRoutingModule} from './app-routing.module';
import {AppService} from './services/app.service';
import {BrowserModule, Title} from '@angular/platform-browser';
import {ConfigService} from './services/config.service';
import {CookieModule} from 'ngx-cookie';
import {FormsModule} from '@angular/forms';
import {HomeComponent} from './pages/home/home.component';
import {HttpClientModule, HTTP_INTERCEPTORS} from '@angular/common/http';
import {HttpResponseInterceptor} from './services/httpResponse.interceptor';
import {LOCALE_ID, NgModule} from '@angular/core';
import {LoginComponent} from './pages/login/login.component';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {StartComponent} from './pages/start/start.component';

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
        console.log(`Stripping variant from window.navigator.language=${window.navigator.language}, using ${result}`);
    }

    const supportedLocales = ['de', 'en', 'es', 'fr', 'sv'];
    if (supportedLocales.indexOf(result) < 0) {
        console.log(`window.navigator.language=${result}, which is not supported. Falling back to en-US`);
        result = 'en-US';
    }
    console.log('bestLocale=%o', result);
    return result;
}

@NgModule({
    imports: [
        AppRoutingModule, BrowserModule, CookieModule.forRoot(), FormsModule, HttpClientModule, NgbModule.forRoot(),
    ],
    declarations: [
        AppComponent,
        HomeComponent,
        LoginComponent,
        StartComponent,
    ],
    providers: [
        ConfigService, Title, AppService, {
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
