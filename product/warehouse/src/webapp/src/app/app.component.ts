import {Component} from '@angular/core';
import {ConfigService} from './config.service';
import {MethodListComponent} from './method-list.component';

@Component({
    selector: '#ck-app',
    template: require('./app.component.html'),
    directives: [MethodListComponent]
})
export class AppComponent {

    now: Date = new Date();
    apiPrefix;
    version;

    constructor(_config: ConfigService) {
        this.apiPrefix = _config.getApiPrefix();
        this.version = _config.getVersion();
    }
}
