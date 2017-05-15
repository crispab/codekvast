import {ActivatedRoute, Router} from '@angular/router';
import {Component, OnInit} from '@angular/core';
import {StateService} from '../services/state.service';
import {AuthState} from './auth.state';

@Component({
    selector: 'ck-sso',
    template: '',
})

export class SsoComponent implements OnInit {

    constructor(private route: ActivatedRoute, private router: Router, private stateService: StateService) {
    }

    ngOnInit(): void {
        let state = this.stateService.getState(AuthState.KEY, () => new AuthState());
        state.setAuthToken(this.route.snapshot.params['token'], this.route.snapshot.params['navData']);

        // noinspection JSIgnoredPromiseFromCall
        this.router.navigate(['']);
    }
}
