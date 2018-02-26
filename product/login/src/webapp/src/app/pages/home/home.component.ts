import {Component, OnInit} from '@angular/core';
import {LoginService} from '../../services/login.service';
import {Router} from '@angular/router';

@Component({
    selector: 'ck-home',
    template: require('./home.component.html')
})
export class HomeComponent implements OnInit {

    constructor(private loginService: LoginService, private router: Router) {
    }

    ngOnInit(): void {
        this.loginService
            .isLoggedIn()
            .do(console.log)
            .subscribe(loggedIn => {
                if (loggedIn) {
                    // noinspection JSIgnoredPromiseFromCall
                    this.router.navigate(['/logged-in']);
                }
            });
    }
}
