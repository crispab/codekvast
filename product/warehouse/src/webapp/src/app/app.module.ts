import {AgePipe} from './age.pipe';
import {APP_BASE_HREF} from '@angular/common';
import {AppComponent} from './app.component';
import {AppRoutingModule} from './app-routing.module';
import {BrowserModule, Title} from '@angular/platform-browser';
import {ConfigService} from './config.service';
import {FormsModule} from '@angular/forms';
import {HomeComponent} from './home.component';
import {HttpModule} from '@angular/http';
import {InvocationStatusPipe} from './invocation-status.pipe';
import {LOCALE_ID, NgModule} from '@angular/core';
import {MethodDetailComponent} from './method-detail.component';
import {MethodsComponent} from './methods.component';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {ReportsComponent} from './reports.component';
import {StateService} from './state.service';
import {StatusComponent} from './status.component';
import {VoteComponent} from './vote.component';
import {WarehouseService} from './warehouse.service';

@NgModule({
    imports: [
        AppRoutingModule, BrowserModule, FormsModule, HttpModule, NgbModule.forRoot(),
    ],
    declarations: [
        AgePipe,
        AppComponent,
        HomeComponent,
        InvocationStatusPipe,
        MethodsComponent,
        MethodDetailComponent,
        ReportsComponent,
        StatusComponent,
        VoteComponent
    ],
    providers: [
        ConfigService,
        Title,
        WarehouseService,
        StateService,
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
