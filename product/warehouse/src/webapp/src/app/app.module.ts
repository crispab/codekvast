import {AgePipe} from './pipes/age.pipe';
import {APP_BASE_HREF} from '@angular/common';
import {AppComponent} from './app.component';
import {AppRoutingModule} from './app-routing.module';
import {BrowserModule, Title} from '@angular/platform-browser';
import {ConfigService} from './services/config.service';
import {FormsModule} from '@angular/forms';
import {HomeComponent} from './pages/home/home.component';
import {HttpModule} from '@angular/http';
import {InvocationStatusPipe} from './pipes/invocation-status.pipe';
import {LOCALE_ID, NgModule} from '@angular/core';
import {MethodDetailComponent} from './pages/methods/method-detail.component';
import {MethodsComponent} from './pages/methods/methods.component';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {ReportGeneratorComponent} from './pages/report-generator/report-generator.component';
import {SettingsComponent} from './components/settings-editor.component';
import {StateService} from './services/state.service';
import {CollectionStatusComponent} from './pages/collection-status/collection-status.component';
import {VoteComponent} from './components/vote.component';
import {WarehouseService} from './services/warehouse.service';
import {VoteResultComponent} from './pages/vote-result/vote-result.component';

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
        MethodDetailComponent,
        MethodsComponent,
        ReportGeneratorComponent,
        SettingsComponent,
        VoteComponent,
        VoteResultComponent,
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
