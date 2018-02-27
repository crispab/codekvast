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
            .isLoggedIn()
            .do(console.log)
            .subscribe(loggedIn => {
                if (loggedIn) {
                    // noinspection JSIgnoredPromiseFromCall
                    this.router.navigateByUrl('start');
                } else {
                    // noinspection JSIgnoredPromiseFromCall
                    this.router.navigateByUrl('login');
                }
            });
    }
}
