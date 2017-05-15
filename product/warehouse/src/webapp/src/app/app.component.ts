import {AfterViewInit, Component, OnInit, ViewEncapsulation} from '@angular/core';
import {ConfigService} from './services/config.service';
import {NavigationEnd, Router} from '@angular/router';
import {TitleCasePipe} from '@angular/common';
import {Title} from '@angular/platform-browser';
import 'rxjs/add/operator/filter';
import 'rxjs/add/operator/map';
import {StateService} from './services/state.service';

@Component({
    selector: '#app',
    template: require('./app.component.html'),
    styles: [require('./app.component.css')],
    encapsulation: ViewEncapsulation.None, // or else styling of html and body won't work in app.component.css
    providers: [TitleCasePipe]
})
export class AppComponent implements OnInit, AfterViewInit {
    constructor(private configService: ConfigService, private titleService: Title, private router: Router,
                private titleCasePipe: TitleCasePipe, private stateService: StateService) {
    }

    ngOnInit(): void {
        this.router.events
            .filter(event => event instanceof NavigationEnd)
            .map(event => (event as NavigationEnd).urlAfterRedirects)
            .subscribe(url => {
                let feature = this.titleCasePipe.transform(url.substr(1));
                this.titleService.setTitle('Codekvast ' + feature)
            })
    }

    ngAfterViewInit(): void {
        // TODO: do if Heroku SSO
        // window['Boomerang'].init({app: 'foo', addon: 'codekvast'});
    }

    getApiPrefix(): String {
        return this.configService.getApiPrefix();
    }

    getVersion(): String {
        return this.configService.getVersion();
    }

    getCurrentUser(): String {
        return this.stateService.getCurrentUser() + ' (' + new Date().getTime() + ')';
    }
}
