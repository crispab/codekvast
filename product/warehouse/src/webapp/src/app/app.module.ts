import {AppComponent} from './app.component';
import {AppRoutingModule} from './app-routing.module';
import {BrowserModule} from '@angular/platform-browser';
import {AgePipe} from './age.pipe';
import {FormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';
import {StatusComponent} from './status.component';
import {MethodsComponent} from './methods.component';
import {MethodDetailComponent} from './method-detail.component';
import {NgModule} from '@angular/core';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {APP_BASE_HREF} from '@angular/common';

@NgModule({
    imports: [BrowserModule, FormsModule, HttpModule, AppRoutingModule, NgbModule.forRoot()],
    declarations: [AppComponent, AgePipe, StatusComponent, MethodsComponent, MethodDetailComponent],
    providers: [{provide: APP_BASE_HREF, useValue : '/' }],
    bootstrap: [AppComponent]
})
export class AppModule {
}
