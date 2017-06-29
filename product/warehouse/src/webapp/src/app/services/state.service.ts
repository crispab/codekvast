/**
 * Injectable holder for persistent state, such as form content, search results etc.
 */
import {Injectable} from '@angular/core';
import {isNullOrUndefined} from 'util';

@Injectable()
export class StateService {

    private readonly AUTH_DATA = 'codekvast.authData';

    private state = {};
    private demoMode = true;

    getState<T>(key: string, initialState: () => T): T {
        if (isNullOrUndefined(this.state[key])) {
            this.state[key] = initialState();
        }
        return this.state[key];
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
            let data = {
                token: token,
                customerId: customerId,
                customerName: customerName,
                email: email,
                source: source,
                sourceApp: sourceApp
            };
            console.log('Setting login data %o', data);
            localStorage.setItem(this.AUTH_DATA, JSON.stringify(data));
        } else {
            console.log('Removing login data');
            localStorage.removeItem(this.AUTH_DATA);
        }
    }

    replaceAuthToken(token: string) {
        let authDataJson = localStorage.getItem(this.AUTH_DATA);
        if (authDataJson) {
            let authData = JSON.parse(authDataJson);
            authData.token = token;
            localStorage.setItem(this.AUTH_DATA, JSON.stringify(authData));
        } else {
            this.setLoggedInAs(token, undefined, undefined, undefined, undefined, undefined);
        }
    }

    setLoggedOut() {
        localStorage.removeItem(this.AUTH_DATA);
    }

    getLoginStateString() {
        if (this.demoMode) {
            return 'Demo mode';
        }

        let authDataJson = localStorage.getItem(this.AUTH_DATA);

        if (authDataJson) {
            let authData = JSON.parse(authDataJson);
            return `Logged in as ${authData.email} / ${authData.customerName}`
        }

        return 'Not logged in';
    }

    getLoginState() {
        if (this.demoMode) {
            return null;
        }
        let authDataJson = localStorage.getItem(this.AUTH_DATA);

        if (authDataJson) {
            let authData = JSON.parse(authDataJson);
            return {
                email: authData.email,
                source: authData.source,
                sourceApp: authData.sourceApp
            }
        }

        return null;
    }

}
