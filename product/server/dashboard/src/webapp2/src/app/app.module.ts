import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AgePipe } from './pipes/age.pipe';
import { InvocationStatusPipe } from './pipes/invocation-status.pipe';

@NgModule({
  declarations: [
    AppComponent,
    AgePipe,
    InvocationStatusPipe
  ],
  imports: [
    BrowserModule,
    AppRoutingModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
