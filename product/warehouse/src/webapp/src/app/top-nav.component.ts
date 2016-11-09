import {Component} from '@angular/core';
import {ConfigService} from './config.service';

@Component({
    selector: 'ck-top-nav',
    template: require('./top-nav.component.html'),
    providers: [ConfigService]
})
export class TopNavComponent {

    constructor(private _config: ConfigService) {
    }

    getApiPrefix(): String {
        return this._config.getApiPrefix();
    }

}
