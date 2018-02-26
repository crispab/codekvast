import {Component, OnInit} from '@angular/core';
import {User} from '../../model/User';
import {LoginService} from '../../services/login.service';

@Component({
    selector: 'ck-logged-in',
    template: require('./logged-in.component.html')
})
export class LoggedInComponent implements OnInit {

    user: User;

    constructor(private loginService: LoginService) {}

    ngOnInit(): void {
        this.loginService.getUser().subscribe(user => this.user = user);
    }

}
