import {Component, OnInit} from '@angular/core';
import {User} from '../../model/User';
import {AppService} from '../../services/app.service';

@Component({
    selector: 'ck-start',
    template: require('./start.component.html')
})
export class StartComponent implements OnInit {

    user: User;

    constructor(private appService: AppService) {
    }

    ngOnInit(): void {
        this.appService.getUser().subscribe(user => this.user = user);
    }

    logout(): void {
        this.appService.logout();
    }
}
