import {Injectable} from '@angular/core';
import {CanActivate} from '@angular/router';
import {WarehouseService} from '../services/warehouse.service';

@Injectable()
export class AuthTokenRenewer implements CanActivate {

    constructor(private warehouseService: WarehouseService) {
    }

    canActivate() {
        return this.warehouseService.ping();
    }
}
