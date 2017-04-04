import {Component, OnInit} from '@angular/core';
import {ConfigService} from './config.service';
import {NavigationEnd, Router} from '@angular/router';
import {TitleCasePipe} from '@angular/common';
import {Title} from '@angular/platform-browser';
import 'rxjs/add/operator/filter';
import 'rxjs/add/operator/map';

@Component({
    selector: '#ck-app',
    template: require('./app.component.html'),
    styles: [require('./app.component.css')],
    providers: [ConfigService, TitleCasePipe]
})
export class AppComponent implements OnInit {
    constructor(private configService: ConfigService,
                private titleService: Title,
                private router: Router,
                private titleCasePipe: TitleCasePipe) {
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

    getApiPrefix(): String {
        return this.configService.getApiPrefix();
    }

    getVersion(): String {
        return this.configService.getVersion();
    }
}
