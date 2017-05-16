/**
 * Injectable holder for persistent state, such as form content, search results etc.
 */
import {Injectable} from '@angular/core';

@Injectable()
export class StateService {

    private readonly AUTH_TOKEN = 'authToken';

    state = {};

    getState<T>(key: string, initialState: () => T): T {
        if (!this.state.hasOwnProperty(key)) {
            this.state[key] = initialState();
        }
        return this.state[key];
    }

    getAuthToken() {
        return localStorage.getItem(this.AUTH_TOKEN);
    }

    setAuthToken(token: string) {
        console.log('Setting auth token %o', token);
        if (token) {
            localStorage.setItem(this.AUTH_TOKEN, token);
        } else {
            localStorage.removeItem(this.AUTH_TOKEN);
        }
    }

    isLoggedIn() {
        let token = this.getAuthToken();
        return !!token;
    }
}
