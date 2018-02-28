import {Component} from '@angular/core';
import {ConfigService} from '../../services/config.service';

@Component({
    selector: 'ck-login',
    template: require('./login.component.html')
})
export class LoginComponent {

    constructor(private config: ConfigService) {}

    getApiPrefix(): String {
        return this.config.getApiPrefix();
    }

}
