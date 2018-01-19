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
        let token = this.route.snapshot.params['token'];
        let parts = token.split('\.');
        if (parts.length >= 2) {
            // header = parts[0]
            let payload = JSON.parse(atob(parts[1]));
            // signature = parts[2]

            let sourceApp = 'unknown';
            if (payload.source === 'heroku') {
                let navData = this.route.snapshot.params['navData'];
                console.log('navData=%o', navData);
                let args = JSON.parse(atob(navData));
                sourceApp = args.app || args.appname;
            }

            this.stateService.setLoggedInAs(payload.customerName, payload.email, payload.source, sourceApp);
        } else {
            this.stateService.setLoggedOut();
        }
        // noinspection JSIgnoredPromiseFromCall
        this.router.navigate(['']);
    }
}
