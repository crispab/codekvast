import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {AgePipe} from './pipes/age.pipe';
import {InvocationStatusPipe} from './pipes/invocation-status.pipe';
import {VoteComponent} from './components/vote/vote.component';
import {SettingsEditorComponent} from './components/settings-editor/settings-editor.component';
import {VoteResultComponent} from './pages/vote-result/vote-result.component';
import {ReportGeneratorComponent} from './pages/report-generator/report-generator.component';
import {CollectionStatusComponent} from './pages/collection-status/collection-status.component';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';

@NgModule({
    declarations: [
        AgePipe,
        AppComponent,
        CollectionStatusComponent,
        InvocationStatusPipe,
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
