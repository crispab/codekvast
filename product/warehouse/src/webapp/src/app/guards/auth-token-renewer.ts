import {CanActivate} from '@angular/router';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {WarehouseService} from '../services/warehouse.service';

@Injectable()
export class AuthTokenRenewer implements CanActivate {

    constructor(private warehouseService: WarehouseService) {
    }

    canActivate() {
        // The role of this routing guard is not to prevent unauthorized routing, but to renew the auth token on every navigation event.
        // Therefore it always return true, even when renewal of the token failed.
        // Token failures are taken care of inside WarehouseService.

        return Observable.create((observer: any) => {
            this.warehouseService.ping().subscribe(() => observer.next(true), () => observer.next(true));
        });
    }
}
