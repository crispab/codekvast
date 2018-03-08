import {Component, OnInit} from '@angular/core';
import {LoginApiService} from '../services/LoginApi.service';
import {Router} from '@angular/router';

@Component({
    selector: 'ck-home',
    template: require('./home.component.html')
})
export class HomeComponent implements OnInit {

    constructor(private api: LoginApiService, private router: Router) {
    }

    ngOnInit(): void {
        this.api
            .isAuthenticated()
            .do(authenticated => console.log(`[ck] Authenticated=${authenticated}`))
            .subscribe(authenticated => {
                // noinspection JSIgnoredPromiseFromCall
                if (authenticated) {
                    // noinspection JSIgnoredPromiseFromCall
                    this.router.navigateByUrl('start');
                } else {
                    // noinspection JSIgnoredPromiseFromCall
                    this.router.navigateByUrl('login');
                }
            });
    }
}
