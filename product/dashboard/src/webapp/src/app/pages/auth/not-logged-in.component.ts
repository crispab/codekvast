import {Component, OnInit} from '@angular/core';
import {DashboardApiService} from '../../services/dashboard-api.service';

@Component({
    selector: 'ck-not-logged-in',
    template: require('./not-logged-in.component.html')
})
export class NotLoggedInComponent implements OnInit {

    private loginUrl: string;

    constructor(private api: DashboardApiService) {
    }

    ngOnInit(): void {
        this.api.getLoginUrl().subscribe(url => this.loginUrl = url);
    }

    getLoginUrl() {
        return this.loginUrl;
    }

}
