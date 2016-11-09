import {AppComponent} from './app.component';
import {BrowserModule} from '@angular/platform-browser';
import {CkAgePipe} from './ck-age.pipe';
import {FormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';
import {MethodListComponent} from './method-list.component';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {NgModule} from '@angular/core';
import {TopNavComponent} from './top-nav.component';

@NgModule({
    imports: [BrowserModule, FormsModule, HttpModule, NgbModule.forRoot()],
    declarations: [AppComponent, TopNavComponent, CkAgePipe, MethodListComponent],
    bootstrap: [AppComponent]
})
export class AppModule {
}
