import {Component, OnInit, ViewEncapsulation} from '@angular/core';
import {ConfigService} from './services/Config.service';
import {NavigationEnd, Router} from '@angular/router';
import {TitleCasePipe} from '@angular/common';
import {Title} from '@angular/platform-browser';
import {LoginApiService} from './services/LoginApi.service';

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

    swaggerUrl = '';

    constructor(private configService: ConfigService, private titleService: Title, private router: Router,
                private titleCasePipe: TitleCasePipe, private api: LoginApiService) {
    }

    ngOnInit(): void {
        this.router.events
            .filter(event => event instanceof NavigationEnd)
            .map(event => (event as NavigationEnd).urlAfterRedirects)
            .subscribe(url => {
                let ga = window['ga'];

                if (!this.googleAnalyticsInitialized) {
                    console.log('[ck login] Initializing GoogleAnalytics');
                    ga('create', this.googleAnalyticsId, 'auto');
                    this.googleAnalyticsInitialized = true;
                }

                let feature = this.titleCasePipe.transform(url.substr(1));
                let hash = feature.indexOf('#');
                if (hash < 0) {
                    hash = feature.length
                }
                feature = feature.substr(0, hash);

                this.titleService.setTitle('Codekvast ' + feature.substr(0, hash));

                console.log(`[ck login] Sending ${feature} to GoogleAnalytics`);
                ga('set', 'page', feature);
                ga('send', 'pageview');
            });
        this.api.getDashboardBaseUrl().subscribe(url => this.swaggerUrl = url + '/swagger-ui.html');
    }

    getVersion(): String {
        return this.configService.getVersion();
    }
}
