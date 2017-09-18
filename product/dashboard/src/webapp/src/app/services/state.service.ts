/**
 * Injectable holder for persistent state, such as form content, search results etc.
 */
import {Injectable} from '@angular/core';
import {isNullOrUndefined} from 'util';
import {Subject} from 'rxjs/Subject';
import {Observable} from 'rxjs/Observable';

export class AuthData {
    readonly token: string;
    readonly customerId: number;
    readonly customerName: string;
    readonly email: string;
    readonly source: string;
    readonly sourceApp: string;

    constructor(token: string, customerId: number, customerName: string, email: string, source: string, sourceApp: string) {
        this.token = token;
        this.customerId = customerId;
        this.customerName = customerName;
        this.email = email;
        this.source = source;
        this.sourceApp = sourceApp;
    }
}

@Injectable()
export class StateService {

    private readonly AUTH_DATA = 'codekvast.authData';

    private state = {};
    private demoMode = true;
    private authData = new Subject<AuthData>();

    getState<T>(key: string, initialState: () => T): T {
        if (isNullOrUndefined(this.state[key])) {
            this.state[key] = initialState();
        }
        return this.state[key];
    }

    getAuthData(): Observable<AuthData> {
        return this.authData;
    }

    getAuthToken() {
        let authDataJson = localStorage.getItem(this.AUTH_DATA);
        return authDataJson ? JSON.parse(authDataJson).token : null;
    }

    isDemoMode() {
        return this.demoMode;
    }

    setDemoMode(demoMode: boolean) {
        this.demoMode = demoMode;
    }

    isLoggedIn() {
        let authData = localStorage.getItem(this.AUTH_DATA);
        return !!authData;
    }

    setLoggedInAs(token: string, customerId: number, customerName: string, email: string, source: string, sourceApp: string) {
        if (token) {
            let authData = new AuthData(token, customerId, customerName, email, source, sourceApp);
            console.log('Setting authData');
            localStorage.setItem(this.AUTH_DATA, JSON.stringify(authData));
            this.authData.next(authData);
        } else {
            this.setLoggedOut();
        }
    }

    replaceAuthToken(token: string) {
        let authDataJson = localStorage.getItem(this.AUTH_DATA);
        if (authDataJson) {
            let authData = JSON.parse(authDataJson);
            authData.token = token;
            console.log('Updating authData');
            localStorage.setItem(this.AUTH_DATA, JSON.stringify(authData));
            this.authData.next(authData);
        } else {
            this.setLoggedInAs(token, undefined, undefined, undefined, undefined, undefined);
        }
    }

    setLoggedOut() {
        console.log('Removing authData');
        localStorage.removeItem(this.AUTH_DATA);
        this.authData.next(null);
    }

    getLoginState() {
        if (this.demoMode) {
            return 'Demo mode';
        }

        let authDataJson = localStorage.getItem(this.AUTH_DATA);

        if (authDataJson) {
            let authData = JSON.parse(authDataJson);
            return `Logged in as ${authData.email}`
        }

        return 'Not logged in';
    }
}
