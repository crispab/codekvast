import {Component} from '@angular/core';
import {ConfigService} from './config.service';

@Component({
    selector: '#ck-app',
    template: require('./app.component.html'),
    styles: [require('./app.component.css')],
    providers: [ConfigService]
})
export class AppComponent {

    constructor(private _config: ConfigService) {
    }

    getApiPrefix(): String {
        return this._config.getApiPrefix();
    }

    getVersion(): String {
        return this._config.getVersion();
    }
}
