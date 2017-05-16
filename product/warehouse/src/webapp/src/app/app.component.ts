import {Component, OnInit, ViewEncapsulation} from '@angular/core';
import {ConfigService} from './services/config.service';
import {NavigationEnd, Router} from '@angular/router';
import {TitleCasePipe} from '@angular/common';
import {Title} from '@angular/platform-browser';
import 'rxjs/add/operator/filter';
import 'rxjs/add/operator/map';
import {LoginState} from './model/login-state';
import {StateService} from './services/state.service';
import {WarehouseService} from './services/warehouse.service';

@Component({
    selector: '#app',
    template: require('./app.component.html'),
    styles: [require('./app.component.css')],
    encapsulation: ViewEncapsulation.None, // or else styling of html and body won't work in app.component.css
    providers: [TitleCasePipe]
})
export class AppComponent implements OnInit {

    constructor(private configService: ConfigService, private stateService: StateService, private titleService: Title,
                private router: Router, private titleCasePipe: TitleCasePipe, private warehouseService: WarehouseService) {
    }

    ngOnInit(): void {
        this.router.events
            .filter(event => event instanceof NavigationEnd)
            .map(event => (event as NavigationEnd).urlAfterRedirects)
            .subscribe(url => {
                let feature = this.titleCasePipe.transform(url.substr(1));
                this.titleService.setTitle('Codekvast ' + feature);
                this.warehouseService.isDemoMode().subscribe(demoMode => this.stateService.demoMode = demoMode);
            });

    }

    getApiPrefix(): String {
        return this.configService.getApiPrefix();
    }

    getVersion(): String {
        return this.configService.getVersion();
    }

    getLoginState() {
        if (this.stateService.demoMode) {
            return 'Demo mode';
        }

        if (this.stateService.isLoggedIn()) {
            let loginState = this.stateService.getState(LoginState.KEY, () => new LoginState());
            return `Logged in as ${loginState.email} / ${loginState.customerName}`
        }

        return 'Not logged in';
    }
}
