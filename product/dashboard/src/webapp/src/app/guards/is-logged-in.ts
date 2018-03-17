import {Injectable, OnInit} from '@angular/core';
import {CanActivate, Router} from '@angular/router';
import {StateService} from '../services/state.service';
import {isNullOrUndefined} from 'util';

@Injectable()
export class IsLoggedIn implements OnInit, CanActivate {

    private isLoggedIn = false;

    constructor(private stateService: StateService, private router: Router) {}

    ngOnInit(): void {
        this.stateService.getAuthData().subscribe(authData => this.isLoggedIn = !isNullOrUndefined(authData));
    }

    canActivate() {
        if (!this.isLoggedIn) {
            // noinspection JSIgnoredPromiseFromCall
            this.router.navigateByUrl('not-logged-in');
        }
        return this.isLoggedIn;
    }

}
