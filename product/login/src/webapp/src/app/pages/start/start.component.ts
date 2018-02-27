import {Component, OnInit} from '@angular/core';
import {LoginAppService} from '../../services/login-app.service';
import {User} from '../../model/User';

@Component({
    selector: 'ck-start',
    template: require('./start.component.html')
})
export class StartComponent implements OnInit {

    user: User;

    constructor(private app: LoginAppService) {
    }

    ngOnInit(): void {
        this.app.getUser().subscribe(user => this.user = user);
    }

    logout(): void {
        this.app.logout();
    }
}
