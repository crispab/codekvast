/**
 * Injectable holder for persistent state, such as form content, search results etc.
 */
import {CookieService} from 'ngx-cookie';
import {Injectable} from '@angular/core';
import {Observable, Subject} from 'rxjs';

export class AuthData {
    readonly customerName: string;
    readonly email: string;
    readonly source: string;
    readonly sourceApp: string;

    constructor(customerName: string, email: string, source: string, sourceApp: string) {
        this.customerName = customerName;
        this.email = email;
        this.source = source;
        this.sourceApp = sourceApp;
    }
}

@Injectable()
export class StateService {

    private state = {};
    private authData = new Subject<AuthData>();
    private loggedIn = false;

    constructor(private cookieService: CookieService) {
    }

    getState<T>(key: string, initialState: () => T): T {
        if (this.isNullOrUndefined(this.state[key])) {
            this.state[key] = initialState();
        }
        return this.state[key];
    }

    getAuthData(): Observable<AuthData> {
        return this.authData;
    }

    setLoggedInAs(customerName: string, email: string, source: string, sourceApp: string) {
        let authData = new AuthData(customerName, email, source, sourceApp);
        this.loggedIn = true;
        this.authData.next(authData);
    }

    setLoggedOut() {
        this.loggedIn = false;
        this.authData.next(null);
        this.cookieService.remove('sessionToken');
        this.cookieService.remove('navData');
    }

    isLoggedIn() {
        if (this.isNullOrUndefined(this.cookieService.get('sessionToken'))) {
            if (this.loggedIn) {
                console.log('[ck dashboard] Detected that sessionToken cookie has disappeared')
            }
            this.setLoggedOut();
        }
        return this.loggedIn;
    }

    private isNullOrUndefined(value: any) {
        return value === null || value === undefined;}
}
