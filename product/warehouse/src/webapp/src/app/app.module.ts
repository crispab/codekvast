import {AppComponent} from './app.component';
import {AppRoutingModule} from './app-routing.module';
import {BrowserModule} from '@angular/platform-browser';
import {CkAgePipe} from './ck-age.pipe';
import {FormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';
import {StatusComponent} from './status.component';
import {MethodsComponent} from './methods.component';
import {NgModule} from '@angular/core';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {TopNavComponent} from './top-nav.component';
import {APP_BASE_HREF} from '@angular/common';

@NgModule({
    imports: [BrowserModule, FormsModule, HttpModule, AppRoutingModule, NgbModule.forRoot()],
    declarations: [AppComponent, CkAgePipe, StatusComponent, MethodsComponent, TopNavComponent],
    providers: [{provide: APP_BASE_HREF, useValue : '/' }],
    bootstrap: [AppComponent]
})
export class AppModule {
}
