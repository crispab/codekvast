import {ActivatedRoute, Router} from '@angular/router';
import {Component, OnInit} from '@angular/core';
import {CookieService} from 'ngx-cookie';
import {StateService} from '../services/state.service';

@Component({
    selector: 'ck-sso',
    template: '',
})

export class SsoComponent implements OnInit {

    constructor(private route: ActivatedRoute, private router: Router, private stateService: StateService,
                private cookieService: CookieService) {
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
            const cookieName = 'sessionToken';
            if (!this.cookieService.get(cookieName)) {
                // This happens when launched from the codekvast-demo1 app, which fails to set the correct sessionToken cookie.
                console.log(`Setting ${cookieName} cookie from path param`);

                let expires = new Date();
                expires.setHours(expires.getHours() + 1);

                this.cookieService.put(cookieName, token, {
                    path: '/',
                    httpOnly: true,
                    expires: expires
                });
            }
        } else {
            this.stateService.setLoggedOut();
        }
        // noinspection JSIgnoredPromiseFromCall
        this.router.navigate(['']);
    }
}
