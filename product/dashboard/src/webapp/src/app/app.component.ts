import {Component, OnInit, ViewEncapsulation} from '@angular/core';
import {ConfigService} from './services/config.service';
import {NavigationEnd, Router} from '@angular/router';
import {StateService} from './services/state.service';
import {TitleCasePipe} from '@angular/common';
import {Title} from '@angular/platform-browser';
import {CookieService} from 'ngx-cookie';

@Component({
    selector: '#app',
    template: require('./app.component.html'),
    styles: [require('./app.component.css')],
    encapsulation: ViewEncapsulation.None, // or else styling of html and body won't work in app.component.css
    providers: [TitleCasePipe]
})
export class AppComponent implements OnInit {

    private showHerokuIntegrationMenu = false;
    private googleAnalyticsInitialized = false;
    private readonly googleAnalyticsId = 'UA-97240168-3';

    constructor(private configService: ConfigService, private stateService: StateService, private titleService: Title,
                private router: Router, private titleCasePipe: TitleCasePipe, private cookieService: CookieService) {
    }

    ngOnInit(): void {
        this.router.events
            .filter(event => event instanceof NavigationEnd)
            .map(event => (event as NavigationEnd).urlAfterRedirects)
            .do(url => {
                console.log('[ck dashboard] NavigationEnd: %o', url);
                this.updateGoogleAnalytics(url);
                this.setLoggedInState();

            })
            .subscribe(url => {
                let feature = this.titleCasePipe.transform(url.substr(1));
                this.titleService.setTitle('Codekvast ' + feature);
            });
    }

    private setLoggedInState() {
        let Boomerang = window['Boomerang'];
        let token = this.cookieService.get('sessionToken') || '';
        let navData = sessionStorage.getItem('navData') || '';

        let parts = token.split('\.');
        if (parts.length >= 2) {
            // header = parts[0]
            let payload = JSON.parse(atob(parts[1]));
            // signature = parts[2]

            let sourceApp = 'codekvast-login';
            if (payload.source === 'heroku') {
                let args = JSON.parse(atob(navData));
                console.log('[ck dashboard] navData=%o', args);
                sourceApp = args.app || args.appname;
                this.showHerokuIntegrationMenu = true;
                Boomerang.init({
                    app: sourceApp,
                    addon: 'codekvast'
                });
            } else {
                this.showHerokuIntegrationMenu = false;
                Boomerang.reset();
                // TODO: show something else
            }
            this.stateService.setLoggedInAs(payload.customerName, payload.email, payload.source, sourceApp);
        } else {
            this.stateService.setLoggedOut();
            this.showHerokuIntegrationMenu = false;
            Boomerang.reset();
        }

    }

    getVersion(): String {
        return this.configService.getVersion();
    }

    getLoginState() {
        return this.stateService.getLoginState();
    }

    topNavClasses() {
        return {
            'integration-menu-heroku': this.showHerokuIntegrationMenu,
            container: true
        }
    }

    private updateGoogleAnalytics(url: string) {
        let ga = window['ga'];

        if (!this.googleAnalyticsInitialized) {
            console.log('[ck dashboard] Initializing GoogleAnalytics');
            ga('create', this.googleAnalyticsId, 'auto');
            this.googleAnalyticsInitialized = true;
        }

        console.log(`[ck dashboard] Sending ${url} to GoogleAnalytics`);
        ga('set', 'page', url);
        ga('send', 'pageview');
    }
}
