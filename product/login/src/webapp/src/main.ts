import {platformBrowserDynamic} from '@angular/platform-browser-dynamic';
import {enableProdMode} from '@angular/core';
import {AppModule} from './app/app.module';

if (process.env.ENV === 'production') {
    enableProdMode();
}
//noinspection JSIgnoredPromiseFromCall
platformBrowserDynamic().bootstrapModule(AppModule);
