import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {CookieService} from 'ngx-cookie';

@Component({
    template: ''
})
export class SsoComponent implements OnInit {
    constructor(private router: Router, private route: ActivatedRoute, private cookieService: CookieService) {
    }

    ngOnInit(): void {
        this.route.params.subscribe(params => {
            console.log('[ck dashboard] SsoComponent.ngOnInit()');
            this.cookieService.put('sessionToken', params['token'], {
                path: '/',
                expires: '-1',
            });
            sessionStorage.setItem('navData', params['navData']);
        });

        // noinspection JSIgnoredPromiseFromCall
        this.router.navigateByUrl('/');
    }
}
