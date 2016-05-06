import {Component} from 'angular2/core';
import {ConfigService} from './config.service';

@Component({
    selector: 'codekvast-warehouse', templateUrl: 'app/app.component.html',
    providers: [ConfigService]
})
export class AppComponent {

    now: Date = new Date();

    constructor(private _config: ConfigService) {
    }

    apiPrefix(): String {
        return this._config.getApiPrefix();
    }

    version(): String {
        return this._config.getVersion();
    }
}
