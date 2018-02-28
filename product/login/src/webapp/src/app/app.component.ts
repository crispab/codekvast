import {Component, OnInit, ViewEncapsulation} from '@angular/core';
import {ConfigService} from './services/config.service';
import {NavigationEnd, Router} from '@angular/router';
import {TitleCasePipe} from '@angular/common';
import {Title} from '@angular/platform-browser';

@Component({
    selector: '#app',
    template: require('./app.component.html'),
    styles: [require('./app.component.css')],
    encapsulation: ViewEncapsulation.None, // or else styling of html and body won't work in app.component.css
    providers: [TitleCasePipe]
})
export class AppComponent implements OnInit {

    private googleAnalyticsInitialized = false;
    private readonly googleAnalyticsId = 'UA-97240168-5';

    constructor(private configService: ConfigService, private titleService: Title, private router: Router,
                private titleCasePipe: TitleCasePipe) {
    }

    ngOnInit(): void {
        this.router.events
            .filter(event => event instanceof NavigationEnd)
            .map(event => (event as NavigationEnd).urlAfterRedirects)
            .do(url => {
                let ga = window['ga'];

                if (!this.googleAnalyticsInitialized) {
                    console.log('Initializing GoogleAnalytics');
                    ga('create', this.googleAnalyticsId, 'auto');
                    this.googleAnalyticsInitialized = true;
                }

                console.log(`Sending ${url} to GoogleAnalytics`);
                ga('set', 'page', url);
                ga('send', 'pageview');
            })
            .subscribe(url => {
                let feature = this.titleCasePipe.transform(url.substr(1));
                this.titleService.setTitle('Codekvast ' + feature);
            });
    }

    getVersion(): String {
        return this.configService.getVersion();
    }
}
