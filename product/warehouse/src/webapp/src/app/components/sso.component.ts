import {ActivatedRoute, Router} from '@angular/router';
import {Component, OnInit} from '@angular/core';
import {StateService} from '../services/state.service';

@Component({
    selector: 'ck-sso',
    template: '',
})

export class SsoComponent implements OnInit {

    constructor(private route: ActivatedRoute, private router: Router, private stateService: StateService) {
    }

    ngOnInit(): void {
        this.stateService.setCurrentUser(this.route.snapshot.params['token']);

        // noinspection JSIgnoredPromiseFromCall
        this.router.navigate(['']);
    }
}
