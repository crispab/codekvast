import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {DashboardApiService} from '../../services/dashboard-api.service';

@Component({
    template: ''
})
export class SsoComponent implements OnInit {
    constructor(private router: Router, private route: ActivatedRoute, private api: DashboardApiService) {
    }

    ngOnInit(): void {
        this.route.params.subscribe(params => {
            let code = params['code'];
            console.log('[ck dashboard] SsoComponent.ngOnInit() code=%o', code);
            this.api.fetchAuthData(code).subscribe( () =>
                // noinspection JSIgnoredPromiseFromCall
                this.router.navigateByUrl('/')
            );
        });

    }
}
