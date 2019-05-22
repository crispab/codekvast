import {AgePipe} from './pipes/age.pipe';
import {AppComponent} from './app.component';
import {AppRoutingModule} from './app-routing.module';
import {BrowserModule} from '@angular/platform-browser';
import {CollectionStatusComponent} from './pages/collection-status/collection-status.component';
import {FormsModule} from '@angular/forms';
import {InvocationStatusPipe} from './pipes/invocation-status.pipe';
import {MethodDetailsComponent} from './pages/methods/method-details/method-details.component';
import {MethodsComponent} from './pages/methods/methods.component';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {NgModule} from '@angular/core';
import {NotLoggedInComponent} from './pages/auth/not-logged-in/not-logged-in.component';
import {ReportGeneratorComponent} from './pages/report-generator/report-generator.component';
import {SettingsEditorComponent} from './components/settings-editor/settings-editor.component';
import {VoteComponent} from './components/vote/vote.component';
import {VoteResultComponent} from './pages/vote-result/vote-result.component';

@NgModule({
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
    ],
    imports: [
        BrowserModule, AppRoutingModule, FormsModule, NgbModule
    ],
    providers: [],
    bootstrap: [AppComponent]
})
export class AppModule {
}
