import {NgModule} from '@angular/core';
import {HttpModule} from '@angular/http';
import {BrowserModule} from '@angular/platform-browser';
import {AppComponent} from './app.component';
import {CkAgePipe} from './ck-age.pipe';
import {ConfigService} from './config.service';
import {MethodListComponent} from './method-list.component';
import {WarehouseService} from './warehouse.service';

@NgModule({
    declarations: [AppComponent, CkAgePipe, ConfigService,MethodListComponent, WarehouseService],
    imports: [BrowserModule, HttpModule],
    bootstrap: [AppComponent]
})
export class AppModule {
}
