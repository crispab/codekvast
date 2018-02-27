import {Component, OnInit} from '@angular/core';
import {AppService} from '../../services/app.service';
import {Router} from '@angular/router';

@Component({
    selector: 'ck-home',
    template: require('./home.component.html')
})
export class HomeComponent implements OnInit {

    constructor(private appService: AppService, private router: Router) {
    }

    ngOnInit(): void {
        this.appService
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
