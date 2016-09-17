import {Component} from '@angular/core';
import {ConfigService} from './config.service';

@Component({
    selector: '#ck-app',
    providers: [ConfigService],
    template: require('./app.component.html'),
})
export class AppComponent {

    now: Date = new Date();
    apiPrefix: String;
    version: String;

    constructor(_config: ConfigService) {
        this.apiPrefix = _config.getApiPrefix();
        this.version = _config.getVersion();
    }
}
