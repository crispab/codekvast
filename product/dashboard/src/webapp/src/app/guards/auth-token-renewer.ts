import {CanActivate} from '@angular/router';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {DashboardService} from '../services/dashboard.service';

@Injectable()
export class AuthTokenRenewer implements CanActivate {

    constructor(private dashboardService: DashboardService) {
    }

    canActivate() {
        // The role of this routing guard is not to prevent unauthorized routing, but to renew the auth token on every navigation event.
        // Therefore it always return true, even when renewal of the token failed.
        // Token failures are taken care of inside DashboardService.

        return Observable.create((observer: any) => {
            this.dashboardService.ping().subscribe(() => observer.next(true), () => observer.next(true));
        });
    }
}
