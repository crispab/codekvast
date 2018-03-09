import {Component, OnInit} from '@angular/core';
import {LoginApiService} from '../services/LoginApi.service';
import {User} from '../model/User';
import {CustomerData} from '../model/CustomerData';

@Component({
    selector: 'ck-start',
    template: require('./start.component.html')
})
export class StartComponent implements OnInit {

    user: User;
    message: string = '';

    constructor(private api: LoginApiService) {
    }

    ngOnInit(): void {
        this.api.getUser().subscribe(user => this.user = user);
    }

    launch(cd: CustomerData): void {
        this.message = this.message + '\nLaunched ' + cd.customerId;
    }

    logout(): void {
        this.api.logout();
    }
}
