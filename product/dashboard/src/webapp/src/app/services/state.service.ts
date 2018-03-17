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

    private state = {};
    private authData = new Subject<AuthData>();
    private loggedIn = false;

    getState<T>(key: string, initialState: () => T): T {
        if (isNullOrUndefined(this.state[key])) {
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
    }

    isLoggedIn() {
        return this.loggedIn;
    }
}
