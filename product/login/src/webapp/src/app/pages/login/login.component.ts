import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

@Component({
    selector: 'ck-login',
    template: require('./login.component.html')
})
export class LoginComponent implements OnInit {

    errorMessage: any;

    constructor(private route: ActivatedRoute) {
    }

    ngOnInit(): void {
        this.route.params.subscribe(params => this.errorMessage = params.errorMessage);
    }
}
