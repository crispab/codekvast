import {CanActivate, Router} from '@angular/router';
import {Injectable} from '@angular/core';
import {StateService} from '../services/state.service';

@Injectable()
export class IsLoggedIn implements CanActivate {

    constructor(private stateService: StateService, private router: Router) {
    }

    canActivate() {
        if (!this.stateService.isLoggedIn()) {
            // noinspection JSIgnoredPromiseFromCall
            this.router.navigateByUrl('not-logged-in');
        }
        return this.stateService.isLoggedIn();
    }

}
