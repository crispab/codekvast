import {Injectable} from '@angular/core';
import {CanActivate, Router} from '@angular/router';
import {StateService} from '../services/state.service';

@Injectable()
export class IsLoggedIn implements CanActivate {

    constructor(private stateService: StateService, private router: Router) {
    }

    canActivate() {
        let result = this.stateService.isLoggedIn();
        if (!result) {
            // noinspection JSIgnoredPromiseFromCall
            this.router.navigateByUrl('not-logged-in');
        }
        return result;
    }
}
