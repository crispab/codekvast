import {Component} from '@angular/core';
import {ConfigService} from './config.service';

@Component({
    selector: '#ck-app',
    template: require('./app.component.html'),
    providers: [ConfigService]
})
export class AppComponent {

    now: Date = new Date();

    constructor(private _config: ConfigService) {
    }

    getApiPrefix(): String {
        return this._config.getApiPrefix();
    }

    getVersion(): String {
        return this._config.getVersion();
    }
}
