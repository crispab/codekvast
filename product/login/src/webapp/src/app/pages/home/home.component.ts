import {Component, OnInit} from '@angular/core';
import {LoginAppService} from '../../services/login-app.service';
import {Router} from '@angular/router';

@Component({
    selector: 'ck-home',
    template: require('./home.component.html')
})
export class HomeComponent implements OnInit {

    constructor(private app: LoginAppService, private router: Router) {
    }

    ngOnInit(): void {
        this.app
            .isAuthenticated()
            .do(authenticated => console.log(`Authenticated=${authenticated}`))
            .subscribe(authenticated => {
                // noinspection JSIgnoredPromiseFromCall
                this.router.navigateByUrl(authenticated ? 'start' : 'login');
            });
    }
}
