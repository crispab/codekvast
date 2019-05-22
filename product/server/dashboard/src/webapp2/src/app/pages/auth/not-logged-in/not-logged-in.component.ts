import {Component} from '@angular/core';
import {ConfigService} from '../../../services/config.service';

@Component({
    selector: 'app-not-logged-in',
    templateUrl: './not-logged-in.component.html',
})
export class NotLoggedInComponent {

    constructor(private config: ConfigService) {
    }

    getLoginUrl() {
        return this.config.getServerSettings().loginUrl;
    }

}

