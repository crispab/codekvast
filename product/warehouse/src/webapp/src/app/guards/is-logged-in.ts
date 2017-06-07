import {Injectable} from '@angular/core';
import {CanActivate} from '@angular/router';
import {StateService} from '../services/state.service';

@Injectable()
export class IsLoggedIn implements CanActivate {

    constructor(private stateService: StateService) {
    }

    canActivate() {
        return this.stateService.isDemoMode() || this.stateService.isLoggedIn();
    }
}
