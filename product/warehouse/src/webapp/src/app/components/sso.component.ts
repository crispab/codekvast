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
        this.stateService.getAuthState().setAuthToken(this.route.snapshot.params['token']);

        let Boomerang = window['Boomerang'];
        let navData = this.route.snapshot.params['navData'];
        if (navData) {
            let args = JSON.parse(atob(navData));
            console.log('navData=%o', args);
            let app = args.app || args.appname;
            Boomerang.init({app: app, addon: 'codekvast'});
        } else {
            Boomerang.reset();
        }

        // noinspection JSIgnoredPromiseFromCall
        this.router.navigate(['']);
    }
}
