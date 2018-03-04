import {Component, OnInit} from '@angular/core';
import {LoginApiService} from '../../services/login-api.service';
import {User} from '../../model/User';

@Component({
    selector: 'ck-start',
    template: require('./start.component.html')
})
export class StartComponent implements OnInit {

    user: User;

    constructor(private api: LoginApiService) {
    }

    ngOnInit(): void {
        this.api.getUser().subscribe(user => this.user = user);
    }

    logout(): void {
        this.api.logout();
    }
}
