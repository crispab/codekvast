import {Injectable} from '@angular/core';
import {CanActivate, Router} from '@angular/router';

import {LoginApiService} from '../services/LoginApi.service';

@Injectable()
export class IsAuthenticated implements CanActivate {

    constructor(private api: LoginApiService, private router: Router) {
    }

    canActivate() {
        return this.api.isAuthenticated().do(authenticated => {
            if (!authenticated) {
                // noinspection JSIgnoredPromiseFromCall
                this.router.navigateByUrl('forbidden');
            }
        });
    }

}
