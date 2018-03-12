/**
 * Injectable holder for persistent state, such as form content, search results etc.
 */
import {Injectable} from '@angular/core';
import {isNullOrUndefined} from 'util';
import {Subject} from 'rxjs/Subject';
import {Observable} from 'rxjs/Observable';

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

    private readonly AUTH_DATA = 'codekvast.authData';

    private state = {};
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

    isLoggedIn() {
        let authData = sessionStorage.getItem(this.AUTH_DATA);
        return !!authData;
    }

    setLoggedInAs(customerName: string, email: string, source: string, sourceApp: string) {
        let authData = new AuthData(customerName, email, source, sourceApp);
        sessionStorage.setItem(this.AUTH_DATA, JSON.stringify(authData));
        this.authData.next(authData);
    }

    setLoggedOut() {
        sessionStorage.removeItem(this.AUTH_DATA);
        this.authData.next(null);
    }

    getLoginState() {
        let authDataJson = sessionStorage.getItem(this.AUTH_DATA);

        if (authDataJson) {
            let authData = JSON.parse(authDataJson);
            return `Logged in as ${authData.email}`
        }

        return 'Not logged in';
    }
}
