import {AppComponent} from './app.component';
import {BrowserModule} from '@angular/platform-browser';
import {CkAgePipe} from './ck-age.pipe';
import {HttpModule} from '@angular/http';
import {MethodListComponent} from './method-list.component';
import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';

@NgModule({
    imports: [BrowserModule, FormsModule, HttpModule],
    declarations: [AppComponent, CkAgePipe, MethodListComponent],
    bootstrap: [AppComponent]
})
export class AppModule {
}
