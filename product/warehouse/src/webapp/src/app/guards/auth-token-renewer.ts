import {Injectable} from '@angular/core';
import {CanActivate} from '@angular/router';
import {WarehouseService} from '../services/warehouse.service';
import {StateService} from '../services/state.service';

@Injectable()
export class AuthTokenRenewer implements CanActivate {

    constructor(private stateService: StateService, private warehouseService: WarehouseService) {
    }

    canActivate() {
        return !this.stateService.isLoggedIn() || this.warehouseService.ping();
    }
}
