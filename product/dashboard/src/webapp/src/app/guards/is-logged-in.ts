import {CanActivate, Router} from '@angular/router';
import {Injectable} from '@angular/core';
import {CookieService} from 'ngx-cookie';
import {isNullOrUndefined} from "util";
import {StateService} from '../services/state.service';

@Injectable()
export class IsLoggedIn implements CanActivate {

    constructor(private cookieService: CookieService, private stateService: StateService, private router: Router) {
    }

    canActivate() {
        let sessionToken = this.cookieService.get('sessionToken');

        if (isNullOrUndefined(sessionToken)) {
            this.stateService.setLoggedOut();
            // noinspection JSIgnoredPromiseFromCall
            this.router.navigateByUrl('not-logged-in');
        }
        return sessionToken != null;
    }

}
