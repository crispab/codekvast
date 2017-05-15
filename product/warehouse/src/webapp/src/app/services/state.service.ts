/**
 * Injectable holder for persistent state, such as form content, search results etc.
 */
import {Injectable} from '@angular/core';

@Injectable()
export class StateService {

    state = {};

    private currentUser: string;

    getState<T>(key: string, initialState: () => T): T {
        if (!this.state.hasOwnProperty(key)) {
            this.state[key] = initialState();
        }
        return this.state[key];
    }

    setCurrentUser(currentUser: string) {
        this.currentUser = currentUser;
        console.log('Current user: %o', currentUser);
    }

    getCurrentUser() {
        return this.currentUser;
    }

    removeCurrentUser() {
        this.currentUser = undefined;
        console.log('Current user is undefined');
    }
}
