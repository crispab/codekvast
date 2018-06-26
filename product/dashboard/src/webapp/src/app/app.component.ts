import {filter, map, tap} from 'rxjs/operators';
import {Component, OnInit, ViewEncapsulation} from '@angular/core';
import {ConfigService} from './services/config.service';
import {NavigationEnd, Router} from '@angular/router';
import {StateService} from './services/state.service';
import {TitleCasePipe} from '@angular/common';
import {Title} from '@angular/platform-browser';
import {CookieService} from 'ngx-cookie';
import {DashboardApiService} from './services/dashboard-api.service';

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
    private Boomerang: any = window['Boomerang'];

    loggedInAs = 'Not logged in';
    viewingCustomer = '';

    private readonly googleAnalyticsId = 'UA-97240168-3';

    constructor(private api: DashboardApiService, private configService: ConfigService, private cookieService: CookieService,
                private router: Router, private stateService: StateService, private titleCasePipe: TitleCasePipe,
                private titleService: Title) {
    }

    ngOnInit(): void {
        this.router.events.pipe(
            filter(event => event instanceof NavigationEnd),
            map(event => (event as NavigationEnd).urlAfterRedirects),
            tap(url => {
                console.log('[ck dashboard] NavigationEnd: %o', url);
                this.updateGoogleAnalytics(url);
                this.setLoggedInState();
            }))
            .subscribe(url => {
                let feature = this.titleCasePipe.transform(url.substr(1));
                this.titleService.setTitle('Codekvast ' + feature);
            });

        this.api.getServerSettings().subscribe(settings => this.configService.setServerSettings(settings));
    }

    private setLoggedInState() {
        let token = this.cookieService.get('sessionToken') || '';
        let navData = this.cookieService.get('navData') || '';

        let parts = token.split('\.');
        if (parts.length >= 2) {
            // header = parts[0]
            let payload = JSON.parse(atob(parts[1]));
            console.log('[ck dashboard] appComponent sessionToken payload=%o', payload);
            // signature = parts[2]

            let sourceApp = 'codekvast-login';
            if (payload.source === 'heroku' && navData && navData.length > 2) {
                let args = JSON.parse(atob(navData));
                console.log('[ck dashboard] appComponent navData=%o', args);
                sourceApp = args.app || args.appname;
                this.showHerokuIntegrationMenu = true;
                this.Boomerang.init({
                    app: sourceApp,
                    addon: 'codekvast'
                });
            } else {
                this.showHerokuIntegrationMenu = false;
                this.Boomerang.reset();
            }
            if (Math.floor(Date.now() / 1000) > payload.exp) {
                console.log('Invalid token, it expired at %o', new Date(payload.exp * 1000));
                this.doLogout();
            } else {
                this.stateService.setLoggedInAs(payload.sub, payload.email, payload.source, sourceApp);
                this.viewingCustomer = 'Viewing data for ' + payload.sub;
                this.loggedInAs = 'Logged in as ' + payload.email;
            }
        } else {
            this.doLogout();
        }
    }

    private doLogout() {
        this.stateService.setLoggedOut();
        this.viewingCustomer = '';
        this.loggedInAs = 'Not logged in';
        this.showHerokuIntegrationMenu = false;
        this.Boomerang.reset();
    }

    getVersion(): String {
        return this.configService.getVersion();
    }

    getServerVersion(): String {
        return this.configService.getServerSettings().serverVersion;
    }

    topNavClasses() {
        return {
            'integration-menu-heroku': this.stateService.isLoggedIn() && this.showHerokuIntegrationMenu,
            'native-login-menu': this.stateService.isLoggedIn() && !this.showHerokuIntegrationMenu,
            container: true
        };
    }

    logout() {
        this.doLogout();
        window.location.href = this.configService.getServerSettings().logoutUrl;
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

    logoutButtonText() {
        return this.stateService.isLoggedIn() ? 'Logout' : 'Login';
    }
}
